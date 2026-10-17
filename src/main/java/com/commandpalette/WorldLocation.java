package com.commandpalette;

import net.runelite.http.api.worlds.WorldRegion;

/**
 * Where a world is: its region, and for the United States which coast, which the region alone doesn't say.
 */
enum WorldLocation
{
	US_EAST("US East", WorldRegion.UNITED_STATES_OF_AMERICA),
	US_WEST("US West", WorldRegion.UNITED_STATES_OF_AMERICA),
	US("US", WorldRegion.UNITED_STATES_OF_AMERICA),
	UK("UK", WorldRegion.UNITED_KINGDOM),
	AUSTRALIA("Australia", WorldRegion.AUSTRALIA),
	GERMANY("Germany", WorldRegion.GERMANY),
	BRAZIL("Brazil", WorldRegion.BRAZIL),
	JAPAN("Japan", WorldRegion.JAPAN),
	SINGAPORE("Singapore", WorldRegion.SINGAPORE),
	SOUTH_AFRICA("South Africa", WorldRegion.SOUTH_AFRICA);

	// the game's numbers for the two coasts, in its enum of world locations, as the World Hopper plugin reads them
	static final int US_WEST_LOCATION = -73;
	static final int US_EAST_LOCATION = -42;

	private final String name;
	private final WorldRegion region;

	WorldLocation(String name, WorldRegion region)
	{
		this.name = name;
		this.region = region;
	}

	String getName()
	{
		return name;
	}

	WorldRegion getRegion()
	{
		return region;
	}

	/**
	 * @param region   the world's region, or null if unknown
	 * @param location the game's location number for the world, or null if it hasn't been read
	 * @return null for an unknown region
	 */
	static WorldLocation of(WorldRegion region, Integer location)
	{
		if (region == null)
		{
			return null;
		}
		if (region == WorldRegion.UNITED_STATES_OF_AMERICA)
		{
			return location == null ? US
				: location == US_WEST_LOCATION ? US_WEST
				: location == US_EAST_LOCATION ? US_EAST
				: US;
		}
		for (WorldLocation each : values())
		{
			if (each.region == region)
			{
				return each;
			}
		}
		return null;
	}
}
