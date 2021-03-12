package syrus.event.decoder;

import java.util.Vector;

/**
 * The string tokenizer class allows an application to break a string into tokens.
 *
 * @author Andres C
 *
 */
public class StringTokenizer
{

    private String dataIn;
    private char initialSeparator;
    private char finalSeparator;
    short position = 0;
    boolean endMethod = false;
    boolean returnOnNoToken = false;
    boolean includeTok = false;

    public StringTokenizer(String dataIn, char separator)
    {
	this.dataIn = dataIn;
	this.initialSeparator = separator;
	this.finalSeparator = separator;
    }

    public StringTokenizer(String dataIn, char separator1, char separator2)
    {
	this.dataIn = dataIn;
	this.initialSeparator = separator1;
	this.finalSeparator = separator2;
    }

    public StringTokenizer(String dataIn, char separator, boolean returnOnNoToken, boolean includeTok)
    {
	this.dataIn = dataIn;
	this.initialSeparator = separator;
	this.finalSeparator = separator;
	this.returnOnNoToken = returnOnNoToken;
	this.includeTok = includeTok;
    }

    /**
     * Returns the next token in this string tokenizer's string. The next token in the string after the current position is returned. The current position
     * is advanced beyond the recognized token. This method does not use the {@link #finalSeparator}
     *
     * @return the next token according to the {@link #initialSeparator}.
     */
    public String nexToken()
    {

	short ptr;
	String aux;

	if (endMethod)
	{
	    return null;
	}

	ptr = (short) dataIn.indexOf(initialSeparator, position);
	if (ptr == -1) // last token, no separator found
	{
	    ptr = (short) dataIn.lastIndexOf(initialSeparator);
	    if (ptr == -1) // last part, no separator found.
	    {
		if (returnOnNoToken == false)
		{
		    return null;
		}
		else if (position == 0)
		{
		    endMethod = true;
		    return dataIn;
		}
		else
		{
		    return null;
		}
	    }
	    aux = dataIn.substring(++ptr);
	    endMethod = true;

	    return aux;
	}

	// separator found
	aux = dataIn.substring(position, ptr);
	position = (short) (ptr + 1);

	if (includeTok)
	{
	    return aux + initialSeparator;
	}

	return aux;
    }

    /**
     * Use this method to get a Vector with tokens between the {@link #initialSeparator} and the {@link #finalSeparator}.
     *
     * @return a Vector with the different tokens or an empty Vector if no tokens are found.
     */
    public Vector getTokensBetweenSeparators()
    {
	Vector resp = new Vector();
	int ptr1 = dataIn.indexOf(initialSeparator);
	int ptr2 = dataIn.indexOf(finalSeparator);
	while (ptr1 != -1 && ptr2 != -1)
	{
	    if (ptr2 > (ptr1 + 1))
	    {
		resp.addElement(dataIn.substring(ptr1 + 1, ptr2));
	    }
	    ptr1 = dataIn.indexOf(initialSeparator, ptr2 + 1);
	    ptr2 = dataIn.indexOf(finalSeparator, ptr2 + 1);
	}

	return resp;
    }
}
