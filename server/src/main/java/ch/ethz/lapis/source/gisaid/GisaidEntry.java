package ch.ethz.lapis.source.gisaid;

import java.time.LocalDate;


public class GisaidEntry {

    private ImportMode importMode;
    private boolean metadataChanged = false;
    private boolean sequenceChanged = false;

    private String gisaidEpiIsl;
    private String strain;
    private LocalDate date;
    private String dateOriginal;
    private String region;
    private String country;
    private String division;
    private String location;
    private String host;
    private Integer age;
    private String sex;
    private String pangoLineage;
    private String gisaidClade;
    private SubmitterInformation submitterInformation;
    private LocalDate dateSubmitted;
    private String samplingStrategy;

    private String seqOriginal;
    private String seqAligned;
    private String geneAASeqs;

    private String aaMutations;
    private String nucSubstitutions;
    private String nucDeletions;
    private String nucInsertions;


    public ImportMode getImportMode() {
        return importMode;
    }

    public GisaidEntry setImportMode(ImportMode importMode) {
        this.importMode = importMode;
        return this;
    }

    public boolean isMetadataChanged() {
        return metadataChanged;
    }

    public GisaidEntry setMetadataChanged(boolean metadataChanged) {
        this.metadataChanged = metadataChanged;
        return this;
    }

    public boolean isSequenceChanged() {
        return sequenceChanged;
    }

    public GisaidEntry setSequenceChanged(boolean sequenceChanged) {
        this.sequenceChanged = sequenceChanged;
        return this;
    }

    public String getGisaidEpiIsl() {
        return gisaidEpiIsl;
    }

    public GisaidEntry setGisaidEpiIsl(String gisaidEpiIsl) {
        this.gisaidEpiIsl = gisaidEpiIsl;
        return this;
    }

    public String getStrain() {
        return strain;
    }

    public GisaidEntry setStrain(String strain) {
        this.strain = strain;
        return this;
    }

    public LocalDate getDate() {
        return date;
    }

    public GisaidEntry setDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public String getDateOriginal() {
        return dateOriginal;
    }

    public GisaidEntry setDateOriginal(String dateOriginal) {
        this.dateOriginal = dateOriginal;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public GisaidEntry setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public GisaidEntry setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getDivision() {
        return division;
    }

    public GisaidEntry setDivision(String division) {
        this.division = division;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public GisaidEntry setLocation(String location) {
        this.location = location;
        return this;
    }

    public String getHost() {
        return host;
    }

    public GisaidEntry setHost(String host) {
        this.host = host;
        return this;
    }

    public Integer getAge() {
        return age;
    }

    public GisaidEntry setAge(Integer age) {
        this.age = age;
        return this;
    }

    public String getSex() {
        return sex;
    }

    public GisaidEntry setSex(String sex) {
        this.sex = sex;
        return this;
    }

    public String getPangoLineage() {
        return pangoLineage;
    }

    public GisaidEntry setPangoLineage(String pangoLineage) {
        this.pangoLineage = pangoLineage;
        return this;
    }

    public String getGisaidClade() {
        return gisaidClade;
    }

    public GisaidEntry setGisaidClade(String gisaidClade) {
        this.gisaidClade = gisaidClade;
        return this;
    }

    public SubmitterInformation getSubmitterInformation() {
        return submitterInformation;
    }

    public GisaidEntry setSubmitterInformation(SubmitterInformation submitterInformation) {
        this.submitterInformation = submitterInformation;
        return this;
    }

    public LocalDate getDateSubmitted() {
        return dateSubmitted;
    }

    public GisaidEntry setDateSubmitted(LocalDate dateSubmitted) {
        this.dateSubmitted = dateSubmitted;
        return this;
    }

    public String getSamplingStrategy() {
        return samplingStrategy;
    }

    public GisaidEntry setSamplingStrategy(String samplingStrategy) {
        this.samplingStrategy = samplingStrategy;
        return this;
    }

    public String getSeqOriginal() {
        return seqOriginal;
    }

    public GisaidEntry setSeqOriginal(String seqOriginal) {
        this.seqOriginal = seqOriginal;
        return this;
    }

    public String getSeqAligned() {
        return seqAligned;
    }

    public GisaidEntry setSeqAligned(String seqAligned) {
        this.seqAligned = seqAligned;
        return this;
    }

    public String getGeneAASeqs() {
        return geneAASeqs;
    }

    public GisaidEntry setGeneAASeqs(String geneAASeqs) {
        this.geneAASeqs = geneAASeqs;
        return this;
    }

    public String getAaMutations() {
        return aaMutations;
    }

    public GisaidEntry setAaMutations(String aaMutations) {
        this.aaMutations = aaMutations;
        return this;
    }

    public String getNucSubstitutions() {
        return nucSubstitutions;
    }

    public GisaidEntry setNucSubstitutions(String nucSubstitutions) {
        this.nucSubstitutions = nucSubstitutions;
        return this;
    }

    public String getNucDeletions() {
        return nucDeletions;
    }

    public GisaidEntry setNucDeletions(String nucDeletions) {
        this.nucDeletions = nucDeletions;
        return this;
    }

    public String getNucInsertions() {
        return nucInsertions;
    }

    public GisaidEntry setNucInsertions(String nucInsertions) {
        this.nucInsertions = nucInsertions;
        return this;
    }
}
