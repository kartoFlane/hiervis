package pl.pwr.basic_hierarchy.interfaces;

public interface DataReader
{
	public Hierarchy load(
		String filePath,
		boolean withInstancesNameAttribute,
		boolean withClassAttribute,
		boolean fillBreadthGaps
	);
}
