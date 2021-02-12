/*
 * This file is generated by jOOQ.
 */
package jhi.gridscore.server.database.codegen.tables.records;


import java.sql.Timestamp;

import javax.annotation.Generated;

import jhi.gridscore.server.database.codegen.tables.Configurations;
import jhi.gridscore.server.pojo.Configuration;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record3;
import org.jooq.Row3;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.9"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class ConfigurationsRecord extends UpdatableRecordImpl<ConfigurationsRecord> implements Record3<String, Configuration, Timestamp> {

    private static final long serialVersionUID = 1148501739;

    /**
     * Setter for <code>gridscore_db.configurations.uuid</code>.
     */
    public void setUuid(String value) {
        set(0, value);
    }

    /**
     * Getter for <code>gridscore_db.configurations.uuid</code>.
     */
    public String getUuid() {
        return (String) get(0);
    }

    /**
     * Setter for <code>gridscore_db.configurations.configuration</code>.
     */
    public void setConfiguration(Configuration value) {
        set(1, value);
    }

    /**
     * Getter for <code>gridscore_db.configurations.configuration</code>.
     */
    public Configuration getConfiguration() {
        return (Configuration) get(1);
    }

    /**
     * Setter for <code>gridscore_db.configurations.created_on</code>.
     */
    public void setCreatedOn(Timestamp value) {
        set(2, value);
    }

    /**
     * Getter for <code>gridscore_db.configurations.created_on</code>.
     */
    public Timestamp getCreatedOn() {
        return (Timestamp) get(2);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<String> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record3 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row3<String, Configuration, Timestamp> fieldsRow() {
        return (Row3) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row3<String, Configuration, Timestamp> valuesRow() {
        return (Row3) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field1() {
        return Configurations.CONFIGURATIONS.UUID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Configuration> field2() {
        return Configurations.CONFIGURATIONS.CONFIGURATION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Timestamp> field3() {
        return Configurations.CONFIGURATIONS.CREATED_ON;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String component1() {
        return getUuid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration component2() {
        return getConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp component3() {
        return getCreatedOn();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value1() {
        return getUuid();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration value2() {
        return getConfiguration();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp value3() {
        return getCreatedOn();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationsRecord value1(String value) {
        setUuid(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationsRecord value2(Configuration value) {
        setConfiguration(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationsRecord value3(Timestamp value) {
        setCreatedOn(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigurationsRecord values(String value1, Configuration value2, Timestamp value3) {
        value1(value1);
        value2(value2);
        value3(value3);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached ConfigurationsRecord
     */
    public ConfigurationsRecord() {
        super(Configurations.CONFIGURATIONS);
    }

    /**
     * Create a detached, initialised ConfigurationsRecord
     */
    public ConfigurationsRecord(String uuid, Configuration configuration, Timestamp createdOn) {
        super(Configurations.CONFIGURATIONS);

        set(0, uuid);
        set(1, configuration);
        set(2, createdOn);
    }
}