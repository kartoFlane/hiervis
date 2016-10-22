package pl.pwr.basic_hierarchy.implementation;

import pl.pwr.basic_hierarchy.interfaces.Instance;


public class BasicInstance implements Instance
{
	private String instanceName;
	private double[] data;
	private String nodeId;
	private String trueClass;


	public BasicInstance( String instanceName, String nodeId, double[] data, String trueClass )
	{
		this.instanceName = instanceName;
		this.nodeId = nodeId;
		this.data = data;
		this.trueClass = trueClass;
	}

	@Override
	public String getNodeId()
	{
		return nodeId;
	}

	@Override
	public double[] getData()
	{
		return data;
	}

	@Override
	public String getTrueClass()
	{
		return trueClass;
	}

	@Override
	public String getInstanceName()
	{
		return instanceName;
	}

	@Override
	public void setNodeId( String nodeId )
	{
		this.nodeId = nodeId;
	}
}
