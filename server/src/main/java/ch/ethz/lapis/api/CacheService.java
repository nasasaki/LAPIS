package ch.ethz.lapis.api;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.controller.v1.SampleController;
import ch.ethz.lapis.api.entity.ApiCacheKey;
import ch.ethz.lapis.api.entity.req.SampleAggregatedRequest;
import ch.ethz.lapis.api.entity.req.SampleDetailRequest;
import ch.ethz.lapis.util.DeflateSeqCompressor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.args.FlushMode;


@Service
@Conditional(IsCacheEnabledCondition.class)
public class CacheService {

    private static final Map<String, Class<?>> endpointToClass = new HashMap<>() {{
        put(SupportedEndpoints.SAMPLE_AGGREGATED, SampleAggregatedRequest.class);
        put(SupportedEndpoints.SAMPLE_AA_MUTATIONS, SampleDetailRequest.class);
        put(SupportedEndpoints.SAMPLE_NUC_MUTATIONS, SampleDetailRequest.class);
    }};
    private static final Map<String, BiConsumer<SampleController, Object>> endpointToPreComputation = new HashMap<>() {{
        put(SupportedEndpoints.SAMPLE_AGGREGATED, (sampleController, obj) -> {
            SampleAggregatedRequest request = (SampleAggregatedRequest) obj;
            sampleController.getAggregated(request);
        });
        put(SupportedEndpoints.SAMPLE_AA_MUTATIONS, (sampleController, obj) -> {
            SampleDetailRequest request = (SampleDetailRequest) obj;
            sampleController.getAAMutations(request);
        });
        put(SupportedEndpoints.SAMPLE_NUC_MUTATIONS, (sampleController, obj) -> {
            SampleDetailRequest request = (SampleDetailRequest) obj;
            sampleController.getNucMutations(request);
        });
    }};
    private final JedisPool pool = new JedisPool(LapisMain.globalConfig.getRedisHost(),
        LapisMain.globalConfig.getRedisPort());
    private final ObjectMapper objectMapper;
    private final DeflateSeqCompressor compressor;
    private final SampleController sampleController;
    public CacheService(@Lazy SampleController sampleController) {
        this.sampleController = sampleController;
        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        compressor = new DeflateSeqCompressor(DeflateSeqCompressor.DICT.NONE);
    }

    public String getCompressedString(ApiCacheKey cacheKey) {
        String keyString = apiCacheKeyToString(cacheKey);
        try (Jedis jedis = pool.getResource()) {
            byte[] compressed = jedis.get(keyString.getBytes(StandardCharsets.UTF_8));
            if (compressed == null) {
                return null;
            }
            return compressor.decompress(compressed);
        }
    }

    public void setCompressedString(ApiCacheKey cacheKey, String value) {
        String keyString = apiCacheKeyToString(cacheKey);
        byte[] compressed = compressor.compress(value);
        try (Jedis jedis = pool.getResource()) {
            jedis.set(keyString.getBytes(StandardCharsets.UTF_8), compressed);
        }
    }

    public Long getLong(String cacheKey) {
        try (Jedis jedis = pool.getResource()) {
            String s = jedis.get(cacheKey);
            if (s == null) {
                return null;
            }
            return Long.parseLong(s);
        }
    }

    public void setLong(String cacheKey, long value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.set(cacheKey, String.valueOf(value));
        }
    }

    /**
     * Clear the cache and recalculate values for all keys if the cache is out-dated.
     */
    public void updateCacheIfOutdated(long newDataVersion) {
        Long currentDataVersion = getCacheDataVersion();
        if (currentDataVersion != null && currentDataVersion == newDataVersion) {
            return;
        }
        List<String> keys = getAllApiKeys();
        try (Jedis jedis = pool.getResource()) {
            jedis.flushAll(FlushMode.SYNC);
        }
        setCacheDataVersion(newDataVersion);

        // Pre-compute
        new Thread(() -> {
            preCompute(keys);
        }).start();
    }

    private List<String> getAllApiKeys() {
        try (Jedis jedis = pool.getResource()) {
            Set<byte[]> keysBytes = jedis.keys("api###*".getBytes(StandardCharsets.UTF_8));
            return keysBytes.stream()
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .collect(Collectors.toList());
        }
    }

    private String apiCacheKeyToString(ApiCacheKey cacheKey) {
        try {
            return "api###" + cacheKey.getEndpointName() + "###"
                + objectMapper.writeValueAsString(cacheKey.getRequestObject());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private Long getCacheDataVersion() {
        return getLong("general###data_version");
    }

    private void setCacheDataVersion(long dataVersion) {
        setLong("general###data_version", dataVersion);
    }

    private void preCompute(List<String> keys) {
        int i = 0;
        for (String key : keys) {
            i++;
            try {
                if (!key.startsWith("api###")) {
                    break;
                }
                String subKey = key.substring(6);
                String[] split = subKey.split("###", 2);
                String endpoint = split[0];
                String requestJson = split[1];
                System.out.println("Pre-computing " + "(" + i + "/" + keys.size() + ") - " + subKey);
                Object request = objectMapper.readValue(requestJson, endpointToClass.get(endpoint));
                endpointToPreComputation.get(endpoint).accept(sampleController, request);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    public static final class SupportedEndpoints {

        public static final String SAMPLE_AGGREGATED = "/v0/sample/aggregated";
        public static final String SAMPLE_AA_MUTATIONS = "/v0/sample/aa-mutations";
        public static final String SAMPLE_NUC_MUTATIONS = "/v0/sample/nuc-mutations";
    }

}
