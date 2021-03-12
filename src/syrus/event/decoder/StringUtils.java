package syrus.event.decoder;

import java.util.Vector;

/**
 * Some String tools.
 *
 * @author juanc - Dct
 *
 */
public class StringUtils
{

    static public Vector getTokens(String in, char tok, boolean includeTok)
    {
        String part;

        Vector out = new Vector();
        StringTokenizer tokens;
        tokens = new StringTokenizer(in, tok, true, includeTok);

        while (true)
        {
            part = tokens.nexToken();

            if (part == null)
            {
                break;
            }
            if (part.equals(""))
            {
                break;
            }
            if (part.length() == 0)
            {
                break;
            }

            out.addElement(part);

        }
        return out;
    }

    static public String[] vectorToStringArray(Vector vect)
    {
        String[] out = new String[vect.size()];
        for (int i = 0; i < vect.size(); i++)
        {
            out[i] = (String) vect.elementAt(i);
        }

        return out;
    }

    static public String[] getTokensStringArray(String in, char tok, boolean includeTok)
    {
        return vectorToStringArray(getTokens(in, tok, includeTok));
    }

    static public String serialStringArray(String[] in)
    {
        String out = "";

        for (int i = 0; i < in.length; i++)
        {
            out += in[i];
        }
        return out;
    }

    static public int[] getTokensIntArray(String in, char tok)
    {
        String part;

        Vector vect = new Vector();

        StringTokenizer tokens = new StringTokenizer(in, tok, true, false);

        int i = 0;
        while (true)
        {
            part = tokens.nexToken();

            if (part == null)
            {
                break;
            }

            i++;
            vect.addElement(part);
        }
        int[] out = new int[vect.size()];
        for (i = 0; i < vect.size(); i++)
        {
            try
            {
                out[i] = Integer.parseInt((String) vect.elementAt(i));
            }
            catch (Exception e)
            {
                out[i] = 0;
            }
        }

        return out;
    }

    static public String cleanText(String str)
    {
        return cleanText(str, false);
    }

    /**
     * Return a clean version of a string by removing its byte values lower than 0x20 and greater than 0x7f
     *
     * @param str
     * @param cleanWhiteSpaces when set to true white spaces (0x20) are removed too
     * @return cleaned version of str
     */
    static public String cleanText(String str, boolean cleanWhiteSpaces)
    {
        if (str == null)
        {
            return null;
        }

        String ret = null;

        int i, j;
        byte[] inbuff = str.getBytes();
        byte[] tempbuff = new byte[inbuff.length];

        for (i = 0, j = 0; i < inbuff.length; i++)
        {
            if (cleanWhiteSpaces == true)
            {
                if (inbuff[i] > 0x20 && inbuff[i] < 0x7f)
                {
                    tempbuff[j] = inbuff[i];
                    j++;
                }
            }
            else if (inbuff[i] >= 0x20 && inbuff[i] < 0x7f)
            {
                tempbuff[j] = inbuff[i];
                j++;
            }
        }

        byte[] outbuff = new byte[j];
        System.arraycopy(tempbuff, 0, outbuff, 0, j);

        ret = new String(outbuff);

        return ret;

    }

    /**
     * Converts a short array into a String. The short array argument must contain a null terminated text
     *
     * @param array
     * @param index
     * @param len   : if 0, then a null terminated text is assumed, else this parameter indicates the length of the returned String
     * @return
     */
    public static String shortArrayToString(short[] array, int index, int len)
    {
        byte[] b = new byte[51];
        int i;

        for (i = 0; array[index] != 0 && i < 50; i++, index++)
        {
            b[i] = (byte) array[index];
        }
        b[i] = 0;

        if (len != 0)
        {
            i = len;
        }

        return new String(b, 0, i);
    }
}
