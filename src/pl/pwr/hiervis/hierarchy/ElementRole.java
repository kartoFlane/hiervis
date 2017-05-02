package pl.pwr.hiervis.hierarchy;

public enum ElementRole
{
	CURRENT( 0 ),
	DIRECT_PARENT( 1 ),
	INDIRECT_PARENT( 2 ),
	CHILD( 3 ),
	OTHER( 4 );

	private final int number;


	private ElementRole( int number )
	{
		this.number = number;
	}

	public int getNumber()
	{
		return this.number;
	}
}
