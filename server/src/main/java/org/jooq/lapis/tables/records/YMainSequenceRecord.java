/*
 * This file is generated by jOOQ.
 */
package org.jooq.lapis.tables.records;


import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record7;
import org.jooq.Row7;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.lapis.tables.YMainSequence;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class YMainSequenceRecord extends UpdatableRecordImpl<YMainSequenceRecord> implements Record7<Integer, byte[], byte[], String, String, String, String> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>y_main_sequence.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>y_main_sequence.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>y_main_sequence.seq_original_compressed</code>.
     */
    public void setSeqOriginalCompressed(byte[] value) {
        set(1, value);
    }

    /**
     * Getter for <code>y_main_sequence.seq_original_compressed</code>.
     */
    public byte[] getSeqOriginalCompressed() {
        return (byte[]) get(1);
    }

    /**
     * Setter for <code>y_main_sequence.seq_aligned_compressed</code>.
     */
    public void setSeqAlignedCompressed(byte[] value) {
        set(2, value);
    }

    /**
     * Getter for <code>y_main_sequence.seq_aligned_compressed</code>.
     */
    public byte[] getSeqAlignedCompressed() {
        return (byte[]) get(2);
    }

    /**
     * Setter for <code>y_main_sequence.aa_mutations</code>.
     */
    public void setAaMutations(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>y_main_sequence.aa_mutations</code>.
     */
    public String getAaMutations() {
        return (String) get(3);
    }

    /**
     * Setter for <code>y_main_sequence.nuc_substitutions</code>.
     */
    public void setNucSubstitutions(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>y_main_sequence.nuc_substitutions</code>.
     */
    public String getNucSubstitutions() {
        return (String) get(4);
    }

    /**
     * Setter for <code>y_main_sequence.nuc_deletions</code>.
     */
    public void setNucDeletions(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>y_main_sequence.nuc_deletions</code>.
     */
    public String getNucDeletions() {
        return (String) get(5);
    }

    /**
     * Setter for <code>y_main_sequence.nuc_insertions</code>.
     */
    public void setNucInsertions(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>y_main_sequence.nuc_insertions</code>.
     */
    public String getNucInsertions() {
        return (String) get(6);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record7 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row7<Integer, byte[], byte[], String, String, String, String> fieldsRow() {
        return (Row7) super.fieldsRow();
    }

    @Override
    public Row7<Integer, byte[], byte[], String, String, String, String> valuesRow() {
        return (Row7) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return YMainSequence.Y_MAIN_SEQUENCE.ID;
    }

    @Override
    public Field<byte[]> field2() {
        return YMainSequence.Y_MAIN_SEQUENCE.SEQ_ORIGINAL_COMPRESSED;
    }

    @Override
    public Field<byte[]> field3() {
        return YMainSequence.Y_MAIN_SEQUENCE.SEQ_ALIGNED_COMPRESSED;
    }

    @Override
    public Field<String> field4() {
        return YMainSequence.Y_MAIN_SEQUENCE.AA_MUTATIONS;
    }

    @Override
    public Field<String> field5() {
        return YMainSequence.Y_MAIN_SEQUENCE.NUC_SUBSTITUTIONS;
    }

    @Override
    public Field<String> field6() {
        return YMainSequence.Y_MAIN_SEQUENCE.NUC_DELETIONS;
    }

    @Override
    public Field<String> field7() {
        return YMainSequence.Y_MAIN_SEQUENCE.NUC_INSERTIONS;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public byte[] component2() {
        return getSeqOriginalCompressed();
    }

    @Override
    public byte[] component3() {
        return getSeqAlignedCompressed();
    }

    @Override
    public String component4() {
        return getAaMutations();
    }

    @Override
    public String component5() {
        return getNucSubstitutions();
    }

    @Override
    public String component6() {
        return getNucDeletions();
    }

    @Override
    public String component7() {
        return getNucInsertions();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public byte[] value2() {
        return getSeqOriginalCompressed();
    }

    @Override
    public byte[] value3() {
        return getSeqAlignedCompressed();
    }

    @Override
    public String value4() {
        return getAaMutations();
    }

    @Override
    public String value5() {
        return getNucSubstitutions();
    }

    @Override
    public String value6() {
        return getNucDeletions();
    }

    @Override
    public String value7() {
        return getNucInsertions();
    }

    @Override
    public YMainSequenceRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public YMainSequenceRecord value2(byte[] value) {
        setSeqOriginalCompressed(value);
        return this;
    }

    @Override
    public YMainSequenceRecord value3(byte[] value) {
        setSeqAlignedCompressed(value);
        return this;
    }

    @Override
    public YMainSequenceRecord value4(String value) {
        setAaMutations(value);
        return this;
    }

    @Override
    public YMainSequenceRecord value5(String value) {
        setNucSubstitutions(value);
        return this;
    }

    @Override
    public YMainSequenceRecord value6(String value) {
        setNucDeletions(value);
        return this;
    }

    @Override
    public YMainSequenceRecord value7(String value) {
        setNucInsertions(value);
        return this;
    }

    @Override
    public YMainSequenceRecord values(Integer value1, byte[] value2, byte[] value3, String value4, String value5, String value6, String value7) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached YMainSequenceRecord
     */
    public YMainSequenceRecord() {
        super(YMainSequence.Y_MAIN_SEQUENCE);
    }

    /**
     * Create a detached, initialised YMainSequenceRecord
     */
    public YMainSequenceRecord(Integer id, byte[] seqOriginalCompressed, byte[] seqAlignedCompressed, String aaMutations, String nucSubstitutions, String nucDeletions, String nucInsertions) {
        super(YMainSequence.Y_MAIN_SEQUENCE);

        setId(id);
        setSeqOriginalCompressed(seqOriginalCompressed);
        setSeqAlignedCompressed(seqAlignedCompressed);
        setAaMutations(aaMutations);
        setNucSubstitutions(nucSubstitutions);
        setNucDeletions(nucDeletions);
        setNucInsertions(nucInsertions);
    }
}
