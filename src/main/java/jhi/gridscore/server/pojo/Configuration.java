package jhi.gridscore.server.pojo;

import java.sql.Timestamp;
import java.util.*;

public class Configuration
{
	private String      uuid;
	private String      name;
	private Integer     cols;
	private Integer     rows;
	private List<Trait> traits;
	private Double[][]  cornerPoints;
	private Markers     markers;
	private Cell[][]    data;
	private Brapi       brapiConfig;
	private Timestamp   lastUpdatedOn;
	private String      comment;
	private DatasetType datasetType = DatasetType.TRIAL;

	public String getUuid()
	{
		return uuid;
	}

	public Configuration setUuid(String uuid)
	{
		this.uuid = uuid;
		return this;
	}

	public String getName()
	{
		return name;
	}

	public Configuration setName(String name)
	{
		this.name = name;
		return this;
	}

	public Integer getCols()
	{
		return cols;
	}

	public Configuration setCols(Integer cols)
	{
		this.cols = cols;
		return this;
	}

	public Integer getRows()
	{
		return rows;
	}

	public Configuration setRows(Integer rows)
	{
		this.rows = rows;
		return this;
	}

	public List<Trait> getTraits()
	{
		return traits;
	}

	public Configuration setTraits(List<Trait> traits)
	{
		this.traits = traits;
		return this;
	}

	public Double[][] getCornerPoints()
	{
		return cornerPoints;
	}

	public Configuration setCornerPoints(Double[][] cornerPoints)
	{
		this.cornerPoints = cornerPoints;
		return this;
	}

	public Markers getMarkers()
	{
		return markers;
	}

	public Configuration setMarkers(Markers markers)
	{
		this.markers = markers;
		return this;
	}

	public Cell[][] getData()
	{
		return data;
	}

	public Configuration setData(Cell[][] data)
	{
		this.data = data;
		return this;
	}

	public Brapi getBrapiConfig()
	{
		return brapiConfig;
	}

	public Configuration setBrapiConfig(Brapi brapiConfig)
	{
		this.brapiConfig = brapiConfig;
		return this;
	}

	public Timestamp getLastUpdatedOn()
	{
		return lastUpdatedOn;
	}

	public Configuration setLastUpdatedOn(Timestamp lastUpdatedOn)
	{
		this.lastUpdatedOn = lastUpdatedOn;
		return this;
	}

	public String getComment()
	{
		return comment;
	}

	public Configuration setComment(String comment)
	{
		this.comment = comment;
		return this;
	}

	public DatasetType getDatasetType()
	{
		// Set default value
		if (datasetType == null)
			return DatasetType.TRIAL;
		else
			return datasetType;
	}

	public Configuration setDatasetType(DatasetType datasetType)
	{
		// Set default value
		if (datasetType == null)
			this.datasetType = DatasetType.TRIAL;
		else
			this.datasetType = datasetType;
		return this;
	}

	@Override
	public String toString()
	{
		return "Configuration{" +
			"uuid='" + uuid + '\'' +
			", name='" + name + '\'' +
			", cols=" + cols +
			", rows=" + rows +
			", traits=" + traits +
			", cornerPoints=" + Arrays.deepToString(cornerPoints) +
			", markers=" + markers +
			", data=" + Arrays.deepToString(data) +
			", brapiConfig=" + brapiConfig +
			", lastUpdatedOn=" + lastUpdatedOn +
			", comment='" + comment + '\'' +
			", datasetType=" + datasetType +
			'}';
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Configuration that = (Configuration) o;
		return Objects.equals(uuid, that.uuid) && name.equals(that.name) && cols.equals(that.cols) && rows.equals(that.rows) && traits.equals(that.traits) && Arrays.deepEquals(cornerPoints, that.cornerPoints) && Objects.equals(markers, that.markers) && Arrays.deepEquals(data, that.data) && Objects.equals(brapiConfig, that.brapiConfig) && Objects.equals(comment, that.comment) && datasetType == that.datasetType;
	}

	@Override
	public int hashCode()
	{
		int result = Objects.hash(uuid, name, cols, rows, traits, markers, brapiConfig, comment, datasetType);
		result = 31 * result + Arrays.deepHashCode(cornerPoints);
		result = 31 * result + Arrays.deepHashCode(data);
		return result;
	}
}
