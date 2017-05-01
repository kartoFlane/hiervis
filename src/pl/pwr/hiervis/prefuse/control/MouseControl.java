package pl.pwr.hiervis.prefuse.control;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.swing.Timer;

import prefuse.controls.ControlAdapter;
import prefuse.visual.VisualItem;


/**
 * Allows for finer control over mouse interaction with displays in prefuse.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class MouseControl extends ControlAdapter
{
	private static final int _clickIntervalMs = (Integer)Toolkit.getDefaultToolkit().getDesktopProperty( "awt.multiClickInterval" );

	private Set<MouseAction> actions = new HashSet<MouseAction>();
	private Queue<Runnable> actionQueue = new LinkedList<Runnable>();
	private Timer timer = null;


	public MouseControl()
	{
		timer = new Timer( _clickIntervalMs, e -> processQueue() );
		timer.setRepeats( false );
	}

	/**
	 * Registers a new mouse action to this control.
	 * 
	 * @param ma
	 *            the MouseAction to register
	 */
	public void addAction( MouseAction ma )
	{
		if ( ma == null ) {
			throw new IllegalArgumentException( "Argument must not be null." );
		}
		actions.add( ma );
	}

	/**
	 * Removes the specified mouse action from this control.
	 * 
	 * @param ma
	 *            the MouseAction to remove
	 */
	public void removeAction( MouseAction ma )
	{
		if ( ma == null ) {
			throw new IllegalArgumentException( "Argument must not be null." );
		}
		actions.remove( ma );
	}

	@Override
	public void mouseClicked( MouseEvent e )
	{
		actionQueue.clear();
		timer.stop();

		if ( e.getClickCount() == 1 ) {
			// Process immediate single-click actions
			actions.stream()
				.filter( ma -> ma.matches( e, TriggerAreaTypes.DISPLAY, true ) )
				.forEach( ma -> ma.execute( null, e ) );
		}

		actions.stream()
			.filter( ma -> ma.matches( e, TriggerAreaTypes.DISPLAY, false ) )
			.forEach( ma -> actionQueue.add( () -> ma.execute( null, e ) ) );

		timer.restart();
	}

	@Override
	public void itemClicked( VisualItem item, MouseEvent e )
	{
		actionQueue.clear();
		timer.stop();

		if ( e.getClickCount() == 1 ) {
			// Process immediate single-click actions
			actions.stream()
				.filter( ma -> ma.matches( e, TriggerAreaTypes.VISUAL_ITEM, true ) )
				.forEach( ma -> ma.execute( item, e ) );
		}

		actions.stream()
			.filter( ma -> ma.matches( e, TriggerAreaTypes.VISUAL_ITEM, false ) )
			.forEach( ma -> actionQueue.add( () -> ma.execute( item, e ) ) );

		timer.restart();
	}

	/**
	 * Processes the queue of pending triggered actions
	 */
	private void processQueue()
	{
		while ( !actionQueue.isEmpty() ) {
			actionQueue.poll().run();
		}
	}


	public static enum TriggerAreaTypes
	{
		/**
		 * The MouseAction will trigger only on clicks on empty Display area, and not visual items.
		 */
		DISPLAY,
		/**
		 * The MouseAction will trigger only on clicks on visual items, and not empty Display area.
		 */
		VISUAL_ITEM,
		/**
		 * The MouseAction will trigger on any clicks on the Display.
		 */
		ANY
	}

	/**
	 * This class is used to associate an action with a set of conditions
	 * that need to be met in order to trigger that action.
	 * 
	 * @author Tomasz Bachmiński
	 *
	 */
	public static class MouseAction
	{
		private BiConsumer<VisualItem, MouseEvent> action;

		private TriggerAreaTypes triggerArea;
		private int clickCount;
		private int button;
		private int modifierMask = 0;
		private boolean immediate = false;


		/**
		 * Creates a new single-click, non-immediate MouseAction with no modifiers and the specified parameters.
		 * 
		 * @param triggerArea
		 *            where the {@code MouseAction} should trigger in the visualization
		 * @param button
		 *            the button that has to be clicked to trigger this action.
		 *            {@link MouseEvent#BUTTON1}, {@link MouseEvent#BUTTON2}, {@link MouseEvent#BUTTON3}
		 * @param action
		 *            the action to perform
		 */
		public MouseAction( TriggerAreaTypes triggerArea, int button, BiConsumer<VisualItem, MouseEvent> action )
		{
			this( triggerArea, button, 1, false, false, false, false, action );
		}

		/**
		 * Creates a new single-click MouseAction with no modifiers and the specified parameters.
		 * 
		 * @param triggerArea
		 *            where the {@code MouseAction} should trigger in the visualization
		 * @param button
		 *            the button that has to be clicked to trigger this action.
		 *            {@link MouseEvent#BUTTON1}, {@link MouseEvent#BUTTON2}, {@link MouseEvent#BUTTON3}
		 * @param immediate
		 *            {@link #setImmediate(boolean)}
		 * @param action
		 *            the action to perform
		 */
		public MouseAction( TriggerAreaTypes triggerArea, int button, boolean immediate, BiConsumer<VisualItem, MouseEvent> action )
		{
			this( triggerArea, button, 1, false, false, false, false, action );
			setImmediate( immediate );
		}

		/**
		 * Creates a new MouseAction with no modifiers and the specified parameters.
		 * 
		 * @param triggerArea
		 *            where the {@code MouseAction} should trigger in the visualization
		 * @param button
		 *            the button that has to be clicked to trigger this action.
		 *            {@link MouseEvent#BUTTON1}, {@link MouseEvent#BUTTON2}, {@link MouseEvent#BUTTON3}
		 * @param clickCount
		 *            number of mouse clicks required to trigger this action
		 * @param action
		 *            the action to perform
		 */
		public MouseAction( TriggerAreaTypes triggerArea, int button, int clickCount, BiConsumer<VisualItem, MouseEvent> action )
		{
			this( triggerArea, button, clickCount, false, false, false, false, action );
		}

		/**
		 * @param triggerArea
		 *            where the {@code MouseAction} should trigger in the visualization
		 * @param button
		 *            the button that has to be clicked to trigger this action.
		 *            {@link MouseEvent#BUTTON1}, {@link MouseEvent#BUTTON2}, {@link MouseEvent#BUTTON3}
		 */
		public MouseAction(
			TriggerAreaTypes triggerArea, int button, int clickCount,
			boolean alt, boolean shift, boolean ctrl, boolean meta, BiConsumer<VisualItem, MouseEvent> action )
		{
			setAction( action );

			setTriggerArea( triggerArea );
			setButton( button );
			setClickCount( clickCount );
			setModifiers( alt, shift, ctrl, meta );
		}

		/**
		 * Makes this action immediate. This method also forces the click count to 1.
		 * 
		 * @param immediate
		 *            if true, this action will be performed immediately when the first mouse click is detected.
		 *            However, this means that it will also be executed when the user double-clicks.
		 *            if false, this action will be performed after the double-click grace time, so that it will
		 *            only be performed when the user actually single-clicks.
		 *            Default: false
		 */
		public void setImmediate( boolean immediate )
		{
			this.immediate = immediate;
			this.clickCount = 1;
		}

		/**
		 * @return whether this action is immediate.
		 * @see #setImmediate(boolean)
		 */
		public boolean isImmediate()
		{
			return immediate;
		}

		/**
		 * Sets the action that is performed when all conditions for this MouseAction are met.
		 * 
		 * @param action
		 *            the action to perform
		 */
		public void setAction( BiConsumer<VisualItem, MouseEvent> action )
		{
			if ( action == null ) {
				throw new IllegalArgumentException( "Action must not be null." );
			}
			this.action = action;
		}

		/**
		 * Sets the trigger area for this MouseAction, ie. where the MouseAction should trigger in the visualization.
		 * 
		 * @param triggerArea
		 *            the trigger area
		 */
		public void setTriggerArea( TriggerAreaTypes triggerArea )
		{
			if ( triggerArea == null ) {
				throw new IllegalArgumentException( "Trigger area must not be null." );
			}
			this.triggerArea = triggerArea;
		}

		/**
		 * Sets the mouse button for this MouseAction
		 * 
		 * @param button
		 *            the mouse button that needs to be pressed to trigger this action
		 */
		public void setButton( int button )
		{
			if ( button < 0 ) {
				throw new IllegalArgumentException( "Button ID must be greater than or equal to 0." );
			}
			this.button = button;
		}

		/**
		 * Sets the click count for this action.
		 * If this action was previously set as immediate, this method will make it NOT immediate, if
		 * the new click count is not 1.
		 * 
		 * @param clickCount
		 *            number of mouse clicks required to trigger this action
		 */
		public void setClickCount( int clickCount )
		{
			if ( clickCount < 1 ) {
				throw new IllegalArgumentException( "Click count must be greater than 0." );
			}
			this.clickCount = clickCount;
			this.immediate &= clickCount == 1;
		}

		/**
		 * @param alt
		 *            if true, Alt key will need to be held down
		 */
		public void setModifierAlt( boolean alt )
		{
			modifierMask = alt
				? ( modifierMask | InputEvent.ALT_DOWN_MASK )
				: ( modifierMask & ~InputEvent.ALT_DOWN_MASK );
		}

		/**
		 * @param shift
		 *            if true, Shift key will need to be held down
		 */
		public void setModifierShift( boolean shift )
		{
			modifierMask = shift
				? ( modifierMask | InputEvent.SHIFT_DOWN_MASK )
				: ( modifierMask & ~InputEvent.SHIFT_DOWN_MASK );
		}

		/**
		 * @param ctrl
		 *            if true, Control key will need to be held down
		 */
		public void setModifierControl( boolean ctrl )
		{
			modifierMask = ctrl
				? ( modifierMask | InputEvent.CTRL_DOWN_MASK )
				: ( modifierMask & ~InputEvent.CTRL_DOWN_MASK );
		}

		/**
		 * @param meta
		 *            if true, Meta key will need to be held down
		 */
		public void setModifierMeta( boolean meta )
		{
			modifierMask = meta
				? ( modifierMask | InputEvent.META_DOWN_MASK )
				: ( modifierMask & ~InputEvent.META_DOWN_MASK );
		}

		/**
		 * Sets modifiers that need to be down while scrolling for zooming to occur.
		 * 
		 * @param alt
		 *            if true, Alt key will need to be held down
		 * @param shift
		 *            if true, Shift key will need to be held down
		 * @param ctrl
		 *            if true, Control key will need to be held down
		 * @param meta
		 *            if true, Meta key will need to be held down
		 */
		public void setModifiers( boolean alt, boolean shift, boolean ctrl, boolean meta )
		{
			modifierMask = 0
				| ( alt ? InputEvent.ALT_DOWN_MASK : 0 )
				| ( shift ? InputEvent.SHIFT_DOWN_MASK : 0 )
				| ( ctrl ? InputEvent.CTRL_DOWN_MASK : 0 )
				| ( meta ? InputEvent.META_DOWN_MASK : 0 );
		}

		/**
		 * @param e
		 *            the current MouseEvent
		 * @param triggerArea
		 *            the trigger area the event happened on
		 * @param immediate
		 *            whether to check immediate or non-immediate actions
		 * @return true if the conditions to trigger this actions are met
		 */
		public boolean matches( MouseEvent e, TriggerAreaTypes triggerArea, boolean immediate )
		{
			if ( this.triggerArea == TriggerAreaTypes.ANY || this.triggerArea == triggerArea ) {
				return this.immediate == immediate && button == e.getButton() && clickCount == e.getClickCount() &&
					( e.getModifiersEx() & modifierMask ) == modifierMask;
			}

			return false;
		}

		/**
		 * Executes this MouseAction's action.
		 */
		public void execute( VisualItem item, MouseEvent e )
		{
			action.accept( item, e );
		}

		@Override
		public boolean equals( Object o )
		{
			if ( o instanceof MouseAction ) {
				MouseAction a = (MouseAction)o;
				return triggerArea == a.triggerArea && clickCount == a.clickCount &&
					button == a.button && modifierMask == a.modifierMask &&
					immediate == a.immediate && action == a.action;
			}
			return false;
		}
	}
}
