package pl.pwr.basic_hierarchy.interfaces;

public interface Instance
{
	public String getInstanceName();

	public String getNodeId();

	public double[] getData();

	public String getTrueClass();

	public void setNodeId( String id );
}
