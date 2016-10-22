package pl.pwr.hiervis.ui.components;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;


@SuppressWarnings("serial")
public class NumberDocument extends PlainDocument
{
	private static final Pattern regex = Pattern.compile( "\\d+" );

	private int limit;


	public NumberDocument( int limit )
	{
		super();
		this.limit = limit;
	}

	public void insertString( int offset, String str, AttributeSet attr )
		throws BadLocationException
	{
		if ( str != null ) {
			Matcher matcher = regex.matcher( str );
			if ( !matcher.matches() ) {
				return;
			}

			if ( ( getLength() + str.length() ) <= limit ) {
				super.insertString( offset, str, attr );
			}
		}
	}
}
