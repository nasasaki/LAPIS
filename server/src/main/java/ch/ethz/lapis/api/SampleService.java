package ch.ethz.lapis.api;

import ch.ethz.lapis.LapisMain;
import ch.ethz.lapis.api.entity.AAMutation;
import ch.ethz.lapis.api.entity.AggregationField;
import ch.ethz.lapis.api.entity.NucMutation;
import ch.ethz.lapis.api.entity.SequenceType;
import ch.ethz.lapis.api.entity.req.OrderAndLimitConfig;
import ch.ethz.lapis.api.entity.req.SampleAggregatedRequest;
import ch.ethz.lapis.api.entity.req.SampleDetailRequest;
import ch.ethz.lapis.api.entity.req.SampleFilter;
import ch.ethz.lapis.api.entity.res.Contributor;
import ch.ethz.lapis.api.entity.res.SampleAggregated;
import ch.ethz.lapis.api.entity.res.SampleDetail;
import ch.ethz.lapis.api.entity.res.SampleMutationsResponse;
import ch.ethz.lapis.api.exception.MalformedVariantQueryException;
import ch.ethz.lapis.api.exception.UnsupportedOrdering;
import ch.ethz.lapis.api.parser.VariantQueryLexer;
import ch.ethz.lapis.api.parser.VariantQueryParser;
import ch.ethz.lapis.api.query.DataStore;
import ch.ethz.lapis.api.query.ThrowingErrorListener;
import ch.ethz.lapis.api.query.VariantQueryExpr;
import ch.ethz.lapis.util.*;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jooq.Record;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.lapis.tables.YMainAaSequenceColumnar;
import org.jooq.lapis.tables.YMainMetadata;
import org.jooq.lapis.tables.YMainSequence;
import org.jooq.lapis.tables.records.YMainSequenceRecord;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Statement;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SampleService {

    private static final ComboPooledDataSource dbPool = LapisMain.dbPool;
    private static final SeqCompressor referenceSeqCompressor
        = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.REFERENCE);
    private static final SeqCompressor columnarCompressor = new ZstdSeqCompressor(ZstdSeqCompressor.DICT.NONE);
    private static final ReferenceGenomeData referenceGenome = ReferenceGenomeData.getInstance();
    private final PangoLineageQueryConverter pangoLineageParser;
    private final DataStore dataStore;


    public SampleService(DataStore dataStore) {
        this.dataStore = dataStore;
        try {
            // TODO This will be only loaded once and will not reload when the aliases change. The aliases should not
            //   change too often so it is not a very big issue but it could potentially cause unexpected results.
            List<PangoLineageAlias> aliases = getPangolinLineageAliases();
            this.pangoLineageParser = new PangoLineageQueryConverter(aliases);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private Connection getDatabaseConnection() throws SQLException {
        return dbPool.getConnection();
    }


    public List<SampleAggregated> getAggregatedSamples(SampleAggregatedRequest request) throws SQLException {
        List<AggregationField> fields = request.getFields();

        Set<Integer> ids = preFilterIds(request);
        if (ids != null && ids.isEmpty()) {
            return new ArrayList<>();
        }

        // Filter further by the other metadata and prepare the response
        List<SampleAggregated> samples = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata tbl = YMainMetadata.Y_MAIN_METADATA;

            List<TableField<?, ?>> groupByFields = getTableFields(fields, tbl);
            List<Field<?>> selectFields = new ArrayList<>(groupByFields);
            selectFields.add(DSL.count().as("count"));
            List<Condition> conditions = getConditions(request, tbl);

            Result<Record> records;
            if (ids != null) {
                Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
                var statement = ctx
                    .select(selectFields)
                    .from(idsTbl.join(tbl).on(idsTbl.field("id", Integer.class).eq(tbl.ID)))
                    .where(conditions)
                    .groupBy(groupByFields);
                records = statement.fetch();
            } else {
                var statement = ctx
                    .select(selectFields)
                    .from(tbl)
                    .where(conditions)
                    .groupBy(groupByFields);
                records = statement.fetch();
            }
            for (var r : records) {
                SampleAggregated sample = new SampleAggregated()
                    .setCount(r.get("count", Integer.class));
                if (fields.contains(AggregationField.DATE)) {
                    sample.setDate(r.get(tbl.DATE));
                }
                if (fields.contains(AggregationField.DATESUBMITTED)) {
                    sample.setDateSubmitted(r.get(tbl.DATE_SUBMITTED));
                }
                if (fields.contains(AggregationField.REGION)) {
                    sample.setRegion(r.get(tbl.REGION));
                }
                if (fields.contains(AggregationField.COUNTRY)) {
                    sample.setCountry(r.get(tbl.COUNTRY));
                }
                if (fields.contains(AggregationField.DIVISION)) {
                    sample.setDivision(r.get(tbl.DIVISION));
                }
                if (fields.contains(AggregationField.LOCATION)) {
                    sample.setLocation(r.get(tbl.LOCATION));
                }
                if (fields.contains(AggregationField.REGIONEXPOSURE)) {
                    sample.setRegionExposure(r.get(tbl.REGION_EXPOSURE));
                }
                if (fields.contains(AggregationField.COUNTRYEXPOSURE)) {
                    sample.setCountryExposure(r.get(tbl.COUNTRY_EXPOSURE));
                }
                if (fields.contains(AggregationField.DIVISIONEXPOSURE)) {
                    sample.setDivisionExposure(r.get(tbl.DIVISION_EXPOSURE));
                }
                if (fields.contains(AggregationField.AGE)) {
                    sample.setAge(r.get(tbl.AGE));
                }
                if (fields.contains(AggregationField.SEX)) {
                    sample.setSex(r.get(tbl.SEX));
                }
                if (fields.contains(AggregationField.HOSPITALIZED)) {
                    sample.setHospitalized(r.get(tbl.HOSPITALIZED));
                }
                if (fields.contains(AggregationField.DIED)) {
                    sample.setDied(r.get(tbl.DIED));
                }
                if (fields.contains(AggregationField.FULLYVACCINATED)) {
                    sample.setFullyVaccinated(r.get(tbl.FULLY_VACCINATED));
                }
                if (fields.contains(AggregationField.HOST)) {
                    sample.setHost(r.get(tbl.HOST));
                }
                if (fields.contains(AggregationField.SAMPLINGSTRATEGY)) {
                    sample.setSamplingStrategy(r.get(tbl.SAMPLING_STRATEGY));
                }
                if (fields.contains(AggregationField.PANGOLINEAGE)) {
                    sample.setPangoLineage(r.get(tbl.PANGO_LINEAGE));
                }
                if (fields.contains(AggregationField.NEXTSTRAINCLADE)) {
                    sample.setNextstrainClade(r.get(tbl.NEXTSTRAIN_CLADE));
                }
                if (fields.contains(AggregationField.GISAIDCLADE)) {
                    sample.setGisaidCloade(r.get(tbl.GISAID_CLADE));
                }
                if (fields.contains(AggregationField.SUBMITTINGLAB)) {
                    sample.setSubmittingLab(r.get(tbl.SUBMITTING_LAB));
                }
                if (fields.contains(AggregationField.ORIGINATINGLAB)) {
                    sample.setOriginatingLab(r.get(tbl.ORIGINATING_LAB));
                }
                samples.add(sample);
            }
        }
        return samples;
    }


    public List<SampleDetail> getDetailedSamples(
        SampleDetailRequest request,
        OrderAndLimitConfig orderAndLimit
    ) throws SQLException {
        Set<Integer> ids = preFilterIds(request);
        if (ids != null && ids.isEmpty()) {
            return new ArrayList<>();
        }

        // Filter further by the other metadata and prepare the response
        List<SampleDetail> samples = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata tbl = YMainMetadata.Y_MAIN_METADATA;

            List<Field<?>> selectFields = new ArrayList<>() {{
                add(tbl.GENBANK_ACCESSION);
                add(tbl.SRA_ACCESSION);
                add(tbl.GISAID_EPI_ISL);
                add(tbl.STRAIN);
                add(tbl.DATE);
                add(tbl.DATE_SUBMITTED);
                add(tbl.REGION);
                add(tbl.COUNTRY);
                add(tbl.DIVISION);
                add(tbl.LOCATION);
                add(tbl.REGION_EXPOSURE);
                add(tbl.COUNTRY_EXPOSURE);
                add(tbl.DIVISION_EXPOSURE);
                add(tbl.AGE);
                add(tbl.SEX);
                add(tbl.HOSPITALIZED);
                add(tbl.DIED);
                add(tbl.FULLY_VACCINATED);
                add(tbl.HOST);
                add(tbl.SAMPLING_STRATEGY);
                add(tbl.PANGO_LINEAGE);
                add(tbl.NEXTSTRAIN_CLADE);
                add(tbl.GISAID_CLADE);
                add(tbl.SUBMITTING_LAB);
                add(tbl.ORIGINATING_LAB);
            }};
            List<Condition> conditions = getConditions(request, tbl);

            Result<Record> records;
            SelectJoinStep<Record> statement;
            if (ids != null) {
                Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
                statement = ctx
                    .select(selectFields)
                    .from(idsTbl.join(tbl).on(idsTbl.field("id", Integer.class).eq(tbl.ID)));
            } else {
                statement = ctx
                    .select(selectFields)
                    .from(tbl);
            }
            SelectConnectByStep<Record> statement2 = statement
                .where(conditions);
            Select<Record> statement3 = applyOrderAndLimit(statement2, orderAndLimit);
            records = statement3.fetch();
            for (var r : records) {
                SampleDetail sample = new SampleDetail()
                    .setGenbankAccession(r.get(tbl.GENBANK_ACCESSION))
                    .setSraAccession(r.get(tbl.SRA_ACCESSION))
                    .setGisaidEpiIsl(r.get(tbl.GISAID_EPI_ISL))
                    .setStrain(r.get(tbl.STRAIN))
                    .setDate(r.get(tbl.DATE))
                    .setDateSubmitted(r.get(tbl.DATE_SUBMITTED))
                    .setRegion(r.get(tbl.REGION))
                    .setCountry(r.get(tbl.COUNTRY))
                    .setDivision(r.get(tbl.DIVISION))
                    .setLocation(r.get(tbl.LOCATION))
                    .setRegionExposure(r.get(tbl.REGION_EXPOSURE))
                    .setCountryExposure(r.get(tbl.COUNTRY_EXPOSURE))
                    .setDivisionExposure(r.get(tbl.DIVISION_EXPOSURE))
                    .setAge(r.get(tbl.AGE))
                    .setSex(r.get(tbl.SEX))
                    .setHospitalized(r.get(tbl.HOSPITALIZED))
                    .setDied(r.get(tbl.DIED))
                    .setFullyVaccinated(r.get(tbl.FULLY_VACCINATED))
                    .setHost(r.get(tbl.HOST))
                    .setSamplingStrategy(r.get(tbl.SAMPLING_STRATEGY))
                    .setPangoLineage(r.get(tbl.PANGO_LINEAGE))
                    .setNextstrainClade(r.get(tbl.NEXTSTRAIN_CLADE))
                    .setGisaidCloade(r.get(tbl.GISAID_CLADE))
                    .setSubmittingLab(r.get(tbl.SUBMITTING_LAB))
                    .setOriginatingLab(r.get(tbl.ORIGINATING_LAB));
                samples.add(sample);
            }
        }
        return samples;
    }


    public List<Contributor> getContributors(
        SampleDetailRequest request,
        OrderAndLimitConfig orderAndLimit
    ) throws SQLException {
        Set<Integer> ids = preFilterIds(request);
        if (ids != null && ids.isEmpty()) {
            return new ArrayList<>();
        }

        // Filter further by the other metadata and prepare the response
        List<Contributor> contributors = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata tbl = YMainMetadata.Y_MAIN_METADATA;

            List<Field<?>> selectFields = new ArrayList<>() {{
                add(tbl.GENBANK_ACCESSION);
                add(tbl.SRA_ACCESSION);
                add(tbl.GISAID_EPI_ISL);
                add(tbl.STRAIN);
                add(tbl.SUBMITTING_LAB);
                add(tbl.ORIGINATING_LAB);
                add(tbl.AUTHORS);
            }};
            List<Condition> conditions = getConditions(request, tbl);

            Result<Record> records;
            SelectJoinStep<Record> statement;
            if (ids != null) {
                Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
                statement = ctx
                    .select(selectFields)
                    .from(idsTbl.join(tbl).on(idsTbl.field("id", Integer.class).eq(tbl.ID)));
            } else {
                statement = ctx
                    .select(selectFields)
                    .from(tbl);
            }
            SelectConnectByStep<Record> statement2 = statement
                .where(conditions);
            Select<Record> statement3 = applyOrderAndLimit(statement2, orderAndLimit);
            records = statement3.fetch();
            for (var r : records) {
                Contributor contributor = new Contributor()
                    .setGenbankAccession(r.get(tbl.GENBANK_ACCESSION))
                    .setSraAccession(r.get(tbl.SRA_ACCESSION))
                    .setGisaidEpiIsl(r.get(tbl.GISAID_EPI_ISL))
                    .setStrain(r.get(tbl.STRAIN))
                    .setSubmittingLab(r.get(tbl.SUBMITTING_LAB))
                    .setOriginatingLab(r.get(tbl.ORIGINATING_LAB))
                    .setAuthors(r.get(tbl.AUTHORS));
                contributors.add(contributor);
            }
        }
        return contributors;
    }


    public List<String> getStrainNames(
        SampleDetailRequest request,
        OrderAndLimitConfig orderAndLimit
    ) throws SQLException {
        Set<Integer> ids = preFilterIds(request);
        if (ids != null && ids.isEmpty()) {
            return new ArrayList<>();
        }

        // Filter further by the other metadata and prepare the response
        List<String> strainNames = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata tbl = YMainMetadata.Y_MAIN_METADATA;

            List<Field<?>> selectFields = new ArrayList<>() {{
                add(tbl.STRAIN);
            }};
            List<Condition> conditions = getConditions(request, tbl);
            conditions.add(tbl.STRAIN.isNotNull());

            Result<Record> records;
            SelectJoinStep<Record> statement;
            if (ids != null) {
                Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
                statement = ctx
                    .select(selectFields)
                    .from(idsTbl.join(tbl).on(idsTbl.field("id", Integer.class).eq(tbl.ID)));
            } else {
                statement = ctx
                    .select(selectFields)
                    .from(tbl);
            }
            SelectConnectByStep<Record> statement2 = statement
                .where(conditions);
            Select<Record> statement3 = applyOrderAndLimit(statement2, orderAndLimit);
            records = statement3.fetch();
            for (var r : records) {
                strainNames.add(r.get(tbl.STRAIN));
            }
        }
        return strainNames;
    }


    public List<String> getGisaidEpiIsls(
        SampleDetailRequest request,
        OrderAndLimitConfig orderAndLimit
    ) throws SQLException {
        Set<Integer> ids = preFilterIds(request);
        if (ids != null && ids.isEmpty()) {
            return new ArrayList<>();
        }

        // Filter further by the other metadata and prepare the response
        List<String> gisaidEpiIsls = new ArrayList<>();
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata tbl = YMainMetadata.Y_MAIN_METADATA;

            List<Field<?>> selectFields = new ArrayList<>() {{
                add(tbl.GISAID_EPI_ISL);
            }};
            List<Condition> conditions = getConditions(request, tbl);
            conditions.add(tbl.GISAID_EPI_ISL.isNotNull());

            Result<Record> records;
            SelectJoinStep<Record> statement;
            if (ids != null) {
                Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
                statement = ctx
                    .select(selectFields)
                    .from(idsTbl.join(tbl).on(idsTbl.field("id", Integer.class).eq(tbl.ID)));
            } else {
                statement = ctx
                    .select(selectFields)
                    .from(tbl);
            }
            SelectConnectByStep<Record> statement2 = statement
                .where(conditions);
            Select<Record> statement3 = applyOrderAndLimit(statement2, orderAndLimit);
            records = statement3.fetch();
            for (var r : records) {
                gisaidEpiIsls.add(r.get(tbl.GISAID_EPI_ISL));
            }
        }
        return gisaidEpiIsls;
    }


    public SampleMutationsResponse getMutations(
        SampleDetailRequest request,
        SequenceType sequenceType,
        float minProportion
    ) throws SQLException {
        Set<Integer> ids = preFilterIds(request);
        if (ids != null && ids.isEmpty()) {
            return new SampleMutationsResponse();
        }

        // Filter further by the other metadata and prepare the response
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata metaTbl = YMainMetadata.Y_MAIN_METADATA;
            YMainSequence seqTbl = YMainSequence.Y_MAIN_SEQUENCE;

            List<Condition> conditions = getConditions(request, metaTbl);
            TableOnConditionStep<Record> baseTbl;
            if (ids != null) {
                Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
                baseTbl = idsTbl
                    .join(metaTbl).on(idsTbl.field("id", Integer.class).eq(metaTbl.ID))
                    .join(seqTbl).on(metaTbl.ID.eq(seqTbl.ID));
            } else {
                baseTbl = metaTbl
                    .join(seqTbl).on(metaTbl.ID.eq(seqTbl.ID));
            }

            var proportionField = DSL.count().cast(Double.class).div(
                DSL.field(
                    ctx
                        .select(DSL.count())
                        .from(baseTbl)
                        .where(conditions)
                )
            ).as("proportion");
            var countField = DSL.count().cast(Integer.class).as("count");
            String mutationColumnName = switch (sequenceType) {
                case AMINO_ACID -> "aa_mutations";
                case NUCLEOTIDE -> "nuc_substitutions || ',' || nuc_deletions";
            };
            var statement = ctx
                .select(
                    DSL.field("mutation").cast(String.class),
                    DSL.field("proportion").cast(Double.class),
                    DSL.field("count").cast(Integer.class)
                )
                .from(
                    ctx
                        .select(
                            DSL.field("mut.mutation"),
                            proportionField,
                            countField
                        )
                        .from(
                            baseTbl.crossApply(
                                "unnest(string_to_array(" + mutationColumnName + ", ',')) mut(mutation)"
                            )
                        )
                        .where(conditions)
                        .groupBy(DSL.field("mut.mutation"))
                )
                .where(DSL.field("proportion").ge(minProportion))
                .orderBy(DSL.field("proportion").desc());
            Result<Record3<String, Double, Integer>> records = statement.fetch();
            List<SampleMutationsResponse.MutationEntry> mutationEntries = new ArrayList<>();
            for (var r : records) {
                // Because of the way we are concatenating the nuc_substitutions and nuc_deletions column, it is
                // possible to get empty values.
                if (r.value1().isBlank()) {
                    continue;
                }
                mutationEntries.add(new SampleMutationsResponse.MutationEntry(
                    r.value1(),
                    r.value2(),
                    r.value3()));
            }
            return new SampleMutationsResponse(mutationEntries);
        }
    }


    public void getFasta(
        SampleDetailRequest request,
        boolean aligned,
        OrderAndLimitConfig orderAndLimit,
        OutputStream outputStream
    ) {
        Set<Integer> ids = preFilterIds(request);
        if (ids != null && ids.isEmpty()) {
            return;
        }

        // Filter further by the other metadata and prepare the response
        Connection conn = null;
        try {
            conn = getDatabaseConnection();
            conn.setAutoCommit(false);
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainMetadata metaTbl = YMainMetadata.Y_MAIN_METADATA;
            YMainSequence seqTbl = YMainSequence.Y_MAIN_SEQUENCE;

            TableField<YMainSequenceRecord, byte[]> seqColumn = aligned ?
                seqTbl.SEQ_ALIGNED_COMPRESSED : seqTbl.SEQ_ORIGINAL_COMPRESSED;

            List<Condition> conditions = getConditions(request, metaTbl);
            SelectLimitPercentStep<Record2<String, byte[]>> statement;
            if (ids != null) {
                Table<Record1<Integer>> idsTbl = getIdsTable(ids, ctx);
                statement = ctx
                    .select(metaTbl.GENBANK_ACCESSION, seqColumn)
                    .from(
                        idsTbl
                            .join(metaTbl).on(idsTbl.field("id", Integer.class).eq(metaTbl.ID))
                            .join(seqTbl).on(metaTbl.ID.eq(seqTbl.ID))
                    )
                    .where(conditions)
                    .limit(orderAndLimit.getLimit() != null ? Math.min(100000, orderAndLimit.getLimit()) : 100000);
            } else {
                statement = ctx
                    .select(metaTbl.GENBANK_ACCESSION, seqColumn)
                    .from(
                        metaTbl
                            .join(seqTbl).on(metaTbl.ID.eq(seqTbl.ID))
                    )
                    .where(conditions)
                    .limit(orderAndLimit.getLimit() != null ? Math.min(100000, orderAndLimit.getLimit()) : 100000);
            }
            Cursor<Record2<String, byte[]>> cursor = statement.fetchSize(1000).fetchLazy();
            for (Record2<String, byte[]> r : cursor) {
                outputStream.write(">".getBytes(StandardCharsets.UTF_8));
                outputStream.write(r.get(metaTbl.GENBANK_ACCESSION).getBytes(StandardCharsets.UTF_8));
                outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                outputStream.write(referenceSeqCompressor.decompress(r.get(seqColumn))
                    .getBytes(StandardCharsets.UTF_8));
                outputStream.write("\n\n".getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }


    /**
     * This function filters either by mutations or by the variantQuery if one of them is requested.
     */
    private Set<Integer> preFilterIds(SampleFilter<?> sampleFilter) {
        try {
            List<NucMutation> nucMutations = sampleFilter.getNucMutations();
            List<AAMutation> aaMutations = sampleFilter.getAaMutations();
            String variantQuery = sampleFilter.getVariantQuery();

            if ((nucMutations != null && !nucMutations.isEmpty())
                || (aaMutations != null && !aaMutations.isEmpty())) {
                if (variantQuery != null) {
                    throw new RuntimeException("It is not allowed to use the nucMutations/aaMutations and variantQuery " +
                        "fields at the same time.");
                }

                return getIdsFromMutationFilters(nucMutations, aaMutations);
            }

            if (variantQuery != null) {
                return getIdsFromVariantQuery(variantQuery);
            }

            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * This function returns a set of IDs of the samples that have the filtered mutations. If an argument (i.e.,
     * nucMutations or aaMutations) is null or empty, it means that we don't filter for that. If we don't filter for any
     * of the two mutation types, this function returns null. If no sequence has the filtered mutations, this function
     * returns an empty set.
     */
    private Set<Integer> getIdsFromMutationFilters(
        List<NucMutation> nucMutations,
        List<AAMutation> aaMutations
    ) throws SQLException {
        // Filter the IDs by nucleotide mutations (if requested)
        List<Integer> nucIds = null;
        if (nucMutations != null && !nucMutations.isEmpty()) {
            nucIds = getIdsWithNucMutations(nucMutations);
            System.out.println("I found " + nucIds.size() + " with the searched " + nucMutations.size()
                + " nucleotide mutations.");
        }
        if (nucIds != null && nucIds.isEmpty()) {
            return new HashSet<>();
        }

        // Filter the IDs by amino acid mutations (if requested)
        List<Integer> aaIds = null;
        if (aaMutations != null && !aaMutations.isEmpty()) {
            aaIds = getIdsWithAAMutations(aaMutations);
            System.out.println("I found " + aaIds.size() + " with the searched " + aaMutations.size()
                + " amino acid mutations.");
        }
        if (aaIds != null && aaIds.isEmpty()) {
            return new HashSet<>();
        }

        // Merge the nuc and aa mutation filter results
        Set<Integer> ids = null;
        if (nucIds != null && aaIds != null) {
            ids = new HashSet<>(nucIds);
            ids.retainAll(new HashSet<>(aaIds));
        } else if (nucIds != null) {
            ids = new HashSet<>(nucIds);
        } else if (aaIds != null) {
            ids = new HashSet<>(aaIds);
        }
        if (ids != null) {
            System.out.println("There are " + ids.size() + " with all the searched nucleotide and amino acid " +
                "mutations");
        }
        return ids;
    }


    /**
     * This function parses and evaluates a variant query.
     */
    private Set<Integer> getIdsFromVariantQuery(String variantQuery) {
        VariantQueryExpr expr;
        try {
            VariantQueryLexer lexer = new VariantQueryLexer(CharStreams.fromString(variantQuery.toUpperCase()));
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            VariantQueryParser parser = new VariantQueryParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(ThrowingErrorListener.INSTANCE);
            ParseTree tree = parser.start();
            ParseTreeWalker walker = new ParseTreeWalker();
            VariantQueryListener listener = new VariantQueryListener();
            walker.walk(listener, tree);
            expr = listener.getExpr();
        } catch (ParseCancellationException e) {
            throw new MalformedVariantQueryException();
        }

        boolean[] filtered = expr.evaluate(dataStore);
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < filtered.length; i++) {
            if (filtered[i]) {
                ids.add(i);
            }
        }
        return new HashSet<>(ids);
    }


    private Table<Record1<Integer>> getIdsTable(Set<Integer> ids, DSLContext ctx) {
        String idsStr = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return ctx
            .select(DSL.field("i.id::integer", Integer.class).as("id"))
            // We are concatenating SQL here!
            // This is safe because the IDs are read from the database and were generated and then written
            // by this program into the database. Further, the IDs are guaranteed to be integers.
            .from("unnest(string_to_array('" + idsStr + "', ',')) i(id)")
            .asTable("ids");
    }


    private List<Condition> getConditions(SampleFilter<?> request, YMainMetadata tbl) {
        List<Condition> conditions = new ArrayList<>();
        if (request.getDateFrom() != null) {
            conditions.add(tbl.DATE.ge(request.getDateFrom()));
        }
        if (request.getDateTo() != null) {
            conditions.add(tbl.DATE.le(request.getDateTo()));
        }
        if (request.getDateSubmittedFrom() != null) {
            conditions.add(tbl.DATE_SUBMITTED.ge(request.getDateSubmittedFrom()));
        }
        if (request.getDateSubmittedTo() != null) {
            conditions.add(tbl.DATE_SUBMITTED.le(request.getDateSubmittedTo()));
        }
        if (request.getRegion() != null) {
            conditions.add(tbl.REGION.eq(request.getRegion()));
        }
        if (request.getCountry() != null) {
            conditions.add(tbl.COUNTRY.eq(request.getCountry()));
        }
        if (request.getDivision() != null) {
            conditions.add(tbl.DIVISION.eq(request.getDivision()));
        }
        if (request.getLocation() != null) {
            conditions.add(tbl.LOCATION.eq(request.getLocation()));
        }
        if (request.getRegionExposure() != null) {
            conditions.add(tbl.REGION_EXPOSURE.eq(request.getRegionExposure()));
        }
        if (request.getCountryExposure() != null) {
            conditions.add(tbl.COUNTRY_EXPOSURE.eq(request.getCountryExposure()));
        }
        if (request.getDivisionExposure() != null) {
            conditions.add(tbl.DIVISION_EXPOSURE.eq(request.getDivisionExposure()));
        }
        if (request.getAgeFrom() != null) {
            conditions.add(tbl.AGE.ge(request.getAgeFrom()));
        }
        if (request.getAgeTo() != null) {
            conditions.add(tbl.AGE.le(request.getAgeTo()));
        }
        if (request.getSex() != null) {
            conditions.add(tbl.SEX.eq(request.getSex()));
        }
        if (request.getHospitalized() != null) {
            conditions.add(tbl.HOSPITALIZED.eq(request.getHospitalized()));
        }
        if (request.getDied() != null) {
            conditions.add(tbl.DIED.eq(request.getDied()));
        }
        if (request.getFullyVaccinated() != null) {
            conditions.add(tbl.FULLY_VACCINATED.eq(request.getFullyVaccinated()));
        }
        if (request.getRegion() != null) {
            conditions.add(tbl.REGION.eq(request.getRegion()));
        }
        if (request.getCountry() != null) {
            conditions.add(tbl.COUNTRY.eq(request.getCountry()));
        }
        if (request.getHost() != null) {
            conditions.add(tbl.HOST.eq(request.getHost()));
        }
        String pangoLineage = request.getPangoLineage();
        if (pangoLineage != null) {
            String[] pangolinLineageLikeStatements = pangoLineageParser.convertToSqlLikes(pangoLineage);
            conditions.add(tbl.PANGO_LINEAGE.like(DSL.any(pangolinLineageLikeStatements)));
        }
        if (request.getRegion() != null) {
            conditions.add(tbl.REGION.eq(request.getRegion()));
        }
        if (request.getCountry() != null) {
            conditions.add(tbl.COUNTRY.eq(request.getCountry()));
        }
        if (request.getNextstrainClade() != null) {
            conditions.add(tbl.NEXTSTRAIN_CLADE.eq(request.getNextstrainClade()));
        }
        if (request.getRegion() != null) {
            conditions.add(tbl.REGION.eq(request.getRegion()));
        }
        if (request.getCountry() != null) {
            conditions.add(tbl.COUNTRY.eq(request.getCountry()));
        }
        if (request.getGisaidClade() != null) {
            conditions.add(tbl.GISAID_CLADE.eq(request.getGisaidClade()));
        }
        if (request.getSubmittingLab() != null) {
            conditions.add(tbl.SUBMITTING_LAB.eq(request.getSubmittingLab()));
        }
        if (request.getOriginatingLab() != null) {
            conditions.add(tbl.ORIGINATING_LAB.eq(request.getOriginatingLab()));
        }
        if (request instanceof SampleDetailRequest) {
            SampleDetailRequest sdr = (SampleDetailRequest) request;
            if (sdr.getGenbankAccession() != null) {
                conditions.add(tbl.GENBANK_ACCESSION.eq(sdr.getGenbankAccession()));
            }
            if (sdr.getSraAccession() != null) {
                conditions.add(tbl.SRA_ACCESSION.eq(sdr.getSraAccession()));
            }
            if (sdr.getGisaidEpiIsl() != null) {
                conditions.add(tbl.GISAID_EPI_ISL.eq(sdr.getGisaidEpiIsl()));
            }
            if (sdr.getStrain() != null) {
                conditions.add(tbl.STRAIN.eq(sdr.getStrain()));
            }
        }
        return conditions;
    }


    private List<TableField<?, ?>> getTableFields(List<AggregationField> aggregationFields, YMainMetadata tbl) {
        Map<AggregationField, TableField<?, ?>> ALL_FIELDS = new HashMap<>() {{
            put(AggregationField.DATE, tbl.DATE);
            put(AggregationField.DATESUBMITTED, tbl.DATE_SUBMITTED);
            put(AggregationField.REGION, tbl.REGION);
            put(AggregationField.COUNTRY, tbl.COUNTRY);
            put(AggregationField.DIVISION, tbl.DIVISION);
            put(AggregationField.LOCATION, tbl.LOCATION);
            put(AggregationField.REGIONEXPOSURE, tbl.REGION_EXPOSURE);
            put(AggregationField.COUNTRYEXPOSURE, tbl.COUNTRY_EXPOSURE);
            put(AggregationField.DIVISIONEXPOSURE, tbl.DIVISION_EXPOSURE);
            put(AggregationField.AGE, tbl.AGE);
            put(AggregationField.SEX, tbl.SEX);
            put(AggregationField.HOSPITALIZED, tbl.HOSPITALIZED);
            put(AggregationField.DIED, tbl.DIED);
            put(AggregationField.FULLYVACCINATED, tbl.FULLY_VACCINATED);
            put(AggregationField.HOST, tbl.HOST);
            put(AggregationField.SAMPLINGSTRATEGY, tbl.SAMPLING_STRATEGY);
            put(AggregationField.PANGOLINEAGE, tbl.PANGO_LINEAGE);
            put(AggregationField.NEXTSTRAINCLADE, tbl.NEXTSTRAIN_CLADE);
            put(AggregationField.GISAIDCLADE, tbl.GISAID_CLADE);
            put(AggregationField.SUBMITTINGLAB, tbl.SUBMITTING_LAB);
            put(AggregationField.ORIGINATINGLAB, tbl.ORIGINATING_LAB);
        }};
        return aggregationFields.stream()
            .map(ALL_FIELDS::get)
            .collect(Collectors.toList());
    }


    private <T extends Record> Select<T> applyOrderAndLimit(
        SelectOrderByStep<T> statement,
        OrderAndLimitConfig orderAndLimitConfig
    ) {
        // orderBy
        SelectLimitStep<T> statement2 = statement;
        String orderBy = orderAndLimitConfig.getOrderBy();
        if (orderBy != null && !orderBy.isBlank() && !orderBy.equals(OrderAndLimitConfig.SpecialOrdering.ARBITRARY)) {
            if (orderBy.equals(OrderAndLimitConfig.SpecialOrdering.RANDOM)) {
                SelectSeekStep1<T, BigDecimal> x;
                statement2 = statement.orderBy(DSL.rand());
            } else {
                throw new UnsupportedOrdering(orderBy);
            }
        }

        // limit
        Select<T> statement3 = statement2;
        if (orderAndLimitConfig.getLimit() != null) {
            statement3 = statement2.limit(orderAndLimitConfig.getLimit());
        }

        return statement3;
    }


    private List<Integer> getIdsWithNucMutations(List<NucMutation> nucMutations) throws SQLException {
        if (nucMutations == null || nucMutations.isEmpty()) {
            throw new RuntimeException("At least one nucleotide mutation must be provided.");
        }
        Map<Integer, NucMutation> positionToMutation = new HashMap<>();
        List<Integer> positions = new ArrayList<>();
        for (NucMutation nucMutation : nucMutations) {
            positionToMutation.put(nucMutation.getPosition(), nucMutation);
            positions.add(nucMutation.getPosition());
        }
        String sql = """
                  select position, data_compressed
                  from y_main_sequence_columnar
                  where position = any(?::int[]);
            """;
        List<Integer> foundIds = null;
        try (Connection conn = getDatabaseConnection()) {
            try (PreparedStatement statement = conn.prepareStatement(sql)) {
                statement.setArray(1, conn.createArrayOf("int", positions.toArray()));
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        int position = rs.getInt("position");
                        NucMutation searchedMutation = positionToMutation.get(position);
                        byte[] compressed = rs.getBytes("data_compressed");
                        char[] nucleotides = columnarCompressor.decompress(compressed).toCharArray();
                        if (foundIds == null) {
                            // If this is the first round, search all sequences
                            foundIds = new ArrayList<>();
                            for (int i = 0; i < nucleotides.length; i++) {
                                if (NucMutation.isMatchingMutation(nucleotides[i], searchedMutation)) {
                                    foundIds.add(i);
                                }
                            }
                        } else {
                            // In the subsequent rounds, we will just continue filter the foundIds.
                            List<Integer> nextFoundIds = new ArrayList<>();
                            for (Integer foundId : foundIds) {
                                if (NucMutation.isMatchingMutation(nucleotides[foundId], searchedMutation)) {
                                    nextFoundIds.add(foundId);
                                }
                            }
                            foundIds = nextFoundIds;
                        }
                        if (foundIds.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        }
        return foundIds;
    }


    private List<Integer> getIdsWithAAMutations(List<AAMutation> aaMutations) throws SQLException {
        if (aaMutations == null || aaMutations.isEmpty()) {
            throw new RuntimeException("At least one amino acid mutation must be provided.");
        }
        // The gene position will be encoded as e.g., S:501
        Map<String, AAMutation> genePositionToMutation = new HashMap<>();
        for (AAMutation aaMutation : aaMutations) {
            genePositionToMutation.put(encodeGenePosition(aaMutation), aaMutation);
        }
        List<Integer> foundIds = null;
        try (Connection conn = getDatabaseConnection()) {
            DSLContext ctx = JooqHelper.getDSLCtx(conn);
            YMainAaSequenceColumnar tbl = YMainAaSequenceColumnar.Y_MAIN_AA_SEQUENCE_COLUMNAR;
            Condition condition = DSL.falseCondition();
            for (AAMutation m : aaMutations) {
                condition = condition.or(tbl.GENE.eq(m.getGene()).and(tbl.POSITION.eq(m.getPosition())));
            }
            var statement = ctx
                .select(tbl.GENE, tbl.POSITION, tbl.DATA_COMPRESSED)
                .from(tbl)
                .where(condition);
            for (Record3<String, Integer, byte[]> record : statement.fetch()) {
                String gene = record.value1();
                int position = record.value2();
                byte[] compressed = record.value3();
                char[] aaCodons = columnarCompressor.decompress(compressed).toCharArray();
                AAMutation searchedMutation = genePositionToMutation.get(encodeGenePosition(gene, position));
                if (foundIds == null) {
                    // If this is the first round, search all sequences
                    foundIds = new ArrayList<>();
                    for (int i = 0; i < aaCodons.length; i++) {
                        if (AAMutation.isMatchingMutation(aaCodons[i], searchedMutation)) {
                            foundIds.add(i);
                        }
                    }
                } else {
                    // In the subsequent rounds, we will just continue filter the foundIds.
                    List<Integer> nextFoundIds = new ArrayList<>();
                    for (Integer foundId : foundIds) {
                        if (AAMutation.isMatchingMutation(aaCodons[foundId], searchedMutation)) {
                            nextFoundIds.add(foundId);
                        }
                    }
                    foundIds = nextFoundIds;
                }
                if (foundIds.isEmpty()) {
                    break;
                }
            }
        }
        return foundIds;
    }


    private String encodeGenePosition(AAMutation aaMutation) {
        return encodeGenePosition(aaMutation.getGene(), aaMutation.getPosition());
    }


    private String encodeGenePosition(String gene, int position) {
        return gene + ":" + position;
    }


    public List<PangoLineageAlias> getPangolinLineageAliases() throws SQLException {
        String sql = """
                    select
                      alias,
                      full_name
                    from pangolin_lineage_alias;
            """;
        try (Connection conn = getDatabaseConnection()) {
            try (Statement statement = conn.createStatement()) {
                try (ResultSet rs = statement.executeQuery(sql)) {
                    List<PangoLineageAlias> aliases = new ArrayList<>();
                    while (rs.next()) {
                        aliases.add(new PangoLineageAlias(
                            rs.getString("alias"),
                            rs.getString("full_name")
                        ));
                    }
                    return aliases;
                }
            }
        }
    }
}
