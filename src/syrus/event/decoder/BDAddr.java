package syrus.event.decoder;

/**
 *
 * @author Andres Cabezas
 */
public class BDAddr
{

    /**
     *
     * @param addr
     * @return a BDAddr object
     */
    public static BDAddr fromString(String addr)
    {
        String[] bytes = StringUtils.getTokensStringArray(addr, ':', false);
        if (bytes.length != 6)
        {
            throw new Error("Invalid Bluetooth address format.");
        }
        byte[] byte_addr = new byte[6];
        for (int i = 0; i < 6; i++)
        {
            byte_addr[5 - i] = (byte) Integer.parseInt(bytes[i], 16);
        }
        return new BDAddr(byte_addr);
    }

    protected byte[] byte_addr;

    /**
     *
     * @return the mac address as byte[]
     */
    public byte[] getByteAddr()
    {
        return byte_addr;
    }

    /**
     *
     * @param addr
     */
    public BDAddr(byte[] addr)
    {
        byte_addr = addr;
    }

    /**
     *
     * @return The mac address like 0A:0B:0C:0D:0E:FF
     */
    public String to_string()
    {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < byte_addr.length; i++)
        {
            result.append(Integer.toHexString((((int) byte_addr[5 - i]) & 0xFF) + 0x100).substring(1));
            if (i < byte_addr.length - 1)
            {
                result.append(":");
            }
        }
        return result.toString().toUpperCase();
    }
}
