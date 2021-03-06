package com.riskgame.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * RiskMap class is the main model for map management. From here the data
 * structure started. This Map is having Map of continents and continent will
 * have territories and those territories will have their adjacent territories.
 * 
 * Two annotations (Getter, Setter) you can see on the top of the class are
 * lombok dependencies to automatically generate getter, setter and tostring..
 * 
 * @author <a href="mailto:z_tel@encs.concordia.ca">Zankhanaben Patel</a>
 */
@Getter
@Setter

public class RiskMap {

	/**
	 * It will represent the name of mapfile.
	 */
	private String mapName;

	/**
	 * This will store all the continents with index.
	 */
	private Map<Integer, Continent> continents;

	/**
	 * It'll represent the status of the map whether it's valid or not.
	 */
	private String status;

	@Override
	public String toString() {
		return "RiskMap [mapName=" + mapName + ", continents=" + continents + ", status=" + status + "]" + "\n";
	}

}
