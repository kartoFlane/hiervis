package pl.pwr.hiervis.core;

import prefuse.Visualization;


public class HVConstants
{
	public static final String PREFUSE_NODE_ID_COLUMN_NAME = "id";
	public static final String PREFUSE_NODE_ROLE_COLUMN_NAME = "role";

	public static final String PREFUSE_INSTANCE_NODE_COLUMN_NAME = "node";
	public static final String PREFUSE_INSTANCE_TRUENODE_COLUMN_NAME = "truenode";
	public static final String PREFUSE_INSTANCE_LABEL_COLUMN_NAME = "label";
	public static final String PREFUSE_INSTANCE_VISIBLE_COLUMN_NAME = "visible";
	public static final String PREFUSE_INSTANCE_AXIS_X_COLUMN_NAME = "labelX";
	public static final String PREFUSE_INSTANCE_AXIS_Y_COLUMN_NAME = "labelY";

	public static final String HIERARCHY_DATA_NAME = "hierarchy";
	public static final String INSTANCE_DATA_NAME = "instances";

	public static final String CSV_FILE_SEPARATOR = ";";
	public static final String HIERARCHY_LEVEL_SEPARATOR = "\\.";

	public static final Visualization EMPTY_VISUALIZATION = new Visualization();

	/*
	 * Instance count constants, used to decide whether we want to sacrifice
	 * some features for better user experience.
	 */
	public static final int INSTANCE_COUNT_MED = 50000;
	public static final int INSTANCE_COUNT_HIGH = 100000;

	/**
	 * Prefuse visualizations use backing tables that contain both original data,
	 * as well as additional data pertaining to the visualization itself.
	 * This constant represents the number of additional columns from the visualization table.
	 * It's useful for when we want to access original data, but can only do that through a
	 * {@link VisualItem} instance.
	 */
	public static final int PREFUSE_VISUAL_TABLE_COLUMN_OFFSET = 34;
}
