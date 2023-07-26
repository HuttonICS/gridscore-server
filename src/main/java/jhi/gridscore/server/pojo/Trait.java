package jhi.gridscore.server.pojo;

import java.util.Objects;

public class Trait
{
	private String       name;
	private String       description;
	private String       type;
	private String       mType;
	private String       brapiId;
	private Restrictions restrictions;

	public String getName()
	{
		return name;
	}

	public Trait setName(String name)
	{
		this.name = name;
		return this;
	}

	public String getDescription()
	{
		return description;
	}

	public Trait setDescription(String description)
	{
		this.description = description;
		return this;
	}

	public String getType()
	{
		return type;
	}

	public Trait setType(String type)
	{
		this.type = type;
		return this;
	}

	public String getmType()
	{
		return mType;
	}

	public Trait setmType(String mType)
	{
		this.mType = mType;
		return this;
	}

	public String getBrapiId()
	{
		return brapiId;
	}

	public Trait setBrapiId(String brapiId)
	{
		this.brapiId = brapiId;
		return this;
	}

	public Restrictions getRestrictions()
	{
		return restrictions;
	}

	public Trait setRestrictions(Restrictions restrictions)
	{
		this.restrictions = restrictions;
		return this;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Trait trait = (Trait) o;
		return name.equals(trait.name) && Objects.equals(description, trait.description) && type.equals(trait.type) && Objects.equals(mType, trait.mType) && Objects.equals(brapiId, trait.brapiId) && Objects.equals(restrictions, trait.restrictions);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, description, type, mType, brapiId, restrictions);
	}

	@Override
	public String toString()
	{
		return "Trait{" +
			"name='" + name + '\'' +
			", description='" + description + '\'' +
			", type='" + type + '\'' +
			", mType='" + mType + '\'' +
			", brapiId='" + brapiId + '\'' +
			", restrictions=" + restrictions +
			'}';
	}
}
