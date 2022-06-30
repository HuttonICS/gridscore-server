package jhi.gridscore.server.pojo;

import java.util.Objects;

public class Trait
{
	private String       name;
	private String       type;
	private String       mType;
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
		return name.equals(trait.name) && type.equals(trait.type) && Objects.equals(mType, trait.mType) && Objects.equals(restrictions, trait.restrictions);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(name, type, mType, restrictions);
	}

	@Override
	public String toString()
	{
		return "Trait{" +
			"name='" + name + '\'' +
			", type='" + type + '\'' +
			", mType='" + mType + '\'' +
			", restrictions=" + restrictions +
			'}';
	}
}
