package jhi.gridscore.server.pojo;

import java.util.Objects;

public class Geolocation
{
	private Double lat;
	private Double lng;
	private Double elv;

	public Double getLat()
	{
		return lat;
	}

	public Geolocation setLat(Double lat)
	{
		this.lat = lat;
		return this;
	}

	public Double getLng()
	{
		return lng;
	}

	public Geolocation setLng(Double lng)
	{
		this.lng = lng;
		return this;
	}

	public Double getElv()
	{
		return elv;
	}

	public Geolocation setElv(Double elv)
	{
		this.elv = elv;
		return this;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Geolocation that = (Geolocation) o;
		return Objects.equals(lat, that.lat) && Objects.equals(lng, that.lng) && Objects.equals(elv, that.elv);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(lat, lng, elv);
	}
}
