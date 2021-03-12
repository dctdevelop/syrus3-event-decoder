/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package syrus.event.decoder;

/**
 *
 * @author syrus
 */
public class Decoder
{

    static final byte LEN_TAIP_FRAME_ZIP = 14;
    static final int IOS_MAX_NBR_GROUPS = 10;
    static final int MAX_NBR_OF_SERIAL_CAMERAS = 10;
    static final int MAX_NBR_OF_SENSORS = 4;

    /**
     * Use this method to get an string with event frame "EV" that includes extended information.
     *
     * @param taipZip The event information coded with {@link Gps.getInstance().getZipPosInfo} and getExtendedInfoCoded method implemented by this class.
     *
     * @return an String with information decoded
     */
    public static String decodeEventWithExtendedInfo(byte[] taipZip)
    {
        int value;
        byte evcode = (byte) ((taipZip[0] >>> 1) & 0x7F);

        System.out.println("Decoding " + taipZip.length + "Bytes");

        if (evcode == 127)
        {
            String aux = new String(taipZip);
            aux = ">R" + aux.substring(1) + "<";
            return (aux + "\n");
        }
        else
        {
            int len = taipZip.length;
            short[] shortData = new short[len];
            for (int i = 0; i < len; i++)
            {
                if (taipZip[i] < 0)
                {
                    shortData[i] = (short) (taipZip[i] & 0x00FF);
                }
                else
                {
                    shortData[i] = (short) taipZip[i];
                }
            }

            int latitude = (((shortData[1] << 8) | shortData[2]) << 8) | shortData[3];
            if ((shortData[0] & 0x01) == 1)
            {
                latitude *= (-1);
            }

            byte dow = (byte) (shortData[4] >>> 5);
            byte sourceAge = getSADecoded((shortData[4] >>> 2) & 0x07);
            int longitude = ((((((shortData[4] & 0x01) << 8) | shortData[5]) << 8) | shortData[6]) << 8) | shortData[7];
            if ((shortData[4] & 0x02) == 2)
            {
                longitude *= (-1);
            }
            short gps_week = (short) (((shortData[8] << 4) | (shortData[9] >>> 4)));
            short speed = (short) (((shortData[9] & 0x0F) << 6) | (shortData[10] >>> 2));
            short heading = (short) (((shortData[10] & 0x03) << 7) | (shortData[11] >>> 1));
            // time of day
            value = ((((shortData[11] & 0x01) << 8) | shortData[12]) << 8) | shortData[13];

            String latitudeEV;
            if (latitude < 0)
            {
                latitudeEV = "-";
            }
            else
            {
                latitudeEV = "+";
            }
            latitudeEV += Utils.integerF(Math.abs(latitude), 7);

            String longitudeEV;
            if (longitude < 0)
            {
                longitudeEV = "-";
            }
            else
            {
                longitudeEV = "+";
            }
            longitudeEV += Utils.integerF(Math.abs(longitude), 8);

            String ev = ">REV" + Utils.integerF(evcode, 2) + Utils.integerF(gps_week, 4) + dow
                    + Utils.integerF(value, 5) + latitudeEV + longitudeEV + Utils.integerF(speed, 3)
                    + Utils.integerF(heading, 3) + Utils.integerF(sourceAge, 2);

            if (len == LEN_TAIP_FRAME_ZIP)
            {
                return (ev + "<\n");
            }
            // Add extended information
            else
            {
                ev += decodeExtendedInfo(taipZip, 14);
            }
            return (ev + "<\n");
        }
    }

    /**
     *
     * @param taipZip the information coded
     * @param initPos where the extended information starts
     *
     * @return an String with the extended information decoded
     */
    private static String decodeExtendedInfo(byte[] taipZip, int initPos)
    {
        int value;
        char ecuSt;
        String toRet = "";

        if (taipZip == null || taipZip.length == 0)
        {
            return null;
        }

        int len = taipZip.length;
        short[] data = new short[len];
        for (int i = 0; i < len; i++)
        {
            if (taipZip[i] < 0)
            {
                data[i] = (short) (taipZip[i] & 0x00FF);
            }
            else
            {
                data[i] = (short) taipZip[i];
            }
        }

        short groupMask = data[initPos];
        short paramMask = data[initPos + 1];
        int i = initPos + 2;

        try
        {
            System.out.println("DecodingGroups " + Integer.toBinaryString(groupMask & 0xFF));
            if ((groupMask & 0x01) == 0x01)
            {// Add byteInfo with mask_1
                System.out.println("DecodingGroup1 " + Integer.toBinaryString(paramMask & 0xFF) + " i=" + i);
                if ((paramMask & 0x01) == 0x01)
                {// Add Virtual Odometer. Old format	
                    data[i] &= 0x7F;
                    value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                    toRet += ";VO=" + value;
                    i += 4;
                }
                if ((paramMask & 0x02) == 0x02)
                {// Add Inputs/Outputs State
                    toRet += ";IO=";
                    // IGN + Main Power supply + Tamper Switch                   
                    //toRet += Integer.toHexString((data[i] & 0x03) | ((data[i] & 0x80) >> 5)).toUpperCase();
                    toRet += Integer.toHexString((data[i] & 0x03)).toUpperCase();
                    // Outputs
                    toRet += Integer.toHexString((data[i] & 0x60) >>> 5).toUpperCase();
                    // Inputs
                    toRet += Integer.toHexString((data[i] & 0x1C) >>> 2).toUpperCase();
                    i++;
                }
                if ((paramMask & 0x04) == 0x04)
                {// Add Satellites in view
                    toRet += ";SV=" + data[i];
                    i++;
                }
                if ((paramMask & 0x08) == 0x08)
                {// Add Battery level
                    value = (data[i] << 8) | data[i + 1];
                    toRet += ";BL=" + value;
                    i += 2;
                }
                if ((paramMask & 0x10) == 0x10)
                {// Add PDOP HDOP and VDOP value
                    if (taipZip[i] == -1)
                    {
                        toRet += ";DOP=-";

                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";DOP=" + value;
                    }
                    if (taipZip[i + 2] == -1)
                    {
                        toRet += ",-";

                    }
                    else
                    {
                        value = (data[i + 2] << 8) | data[i + 3];
                        toRet += "," + value;
                    }
                    if (taipZip[i + 4] == -1)
                    {
                        toRet += ",-";

                    }
                    else
                    {
                        value = (data[i + 4] << 8) | data[i + 5];
                        toRet += "," + value;
                    }
                    i += 6;
                }
                if ((paramMask & 0x20) == 0x20)
                {// Add cell information
                    value = (data[i] << 8) | data[i + 1];
                    toRet += ";CF=" + Integer.toHexString(value).toUpperCase();
                    value = (data[i + 2] << 8) | data[i + 3];
                    toRet += "," + Integer.toHexString(value).toUpperCase();
                    toRet += "," + data[i + 4];
                    i += 5;
                }
                if ((paramMask & 0x40) == 0x40)
                {// Add Counter value
                    do
                    {
                        value = ((((data[i] & 0x01) << 8) | data[i + 1]) << 8) | data[i + 2];
                        toRet += ";CV" + Utils.integerF((data[i] & 0x7E) >>> 1, 2) + "=" + value;
                        i += 3;
                    }
                    while ((data[i - 3] & 0x80) == 0x80);
                }
                //#if _1W_INTERFACE == 1
                if ((paramMask & 0x80) == 0x80)
                {// Add analog interface information
                    value = (data[i] << 8) | data[i + 1];
                    if (value == 9999)
                    {
                        toRet += ";EA=A*";
                    }
                    else
                    {
                        toRet += ";EA=A" + value;
                    }
                    value = (data[i + 2] << 8) | data[i + 3];
                    if (value == 9999)
                    {
                        toRet += "B*";
                    }
                    else
                    {
                        toRet += "B" + value;
                    }
                    value = (data[i + 4] << 8) | data[i + 5];
                    if (value == 9999)
                    {
                        toRet += "C*";
                    }
                    else
                    {
                        toRet += "C" + value;
                    }
                    i += 6;
                }
                //#endif

            }
            if ((groupMask & 0x02) == 0x02)
            {// Add byteInfo with mask_2
                short mask_2;
                if (i == (initPos + 2))
                {
                    mask_2 = paramMask;
                }
                else
                {
                    mask_2 = data[i];
                    i++;
                }
                System.out.println("DecodingGroup2 " + Integer.toBinaryString(mask_2 & 0xFF) + " i=" + i);
                //#if _ANALOG_INPUT == 1
                if ((mask_2 & 0x01) == 0x01)
                {// Add ADC information
                    value = (data[i] << 8) | data[i + 1];
                    toRet += ";AD=" + value;
                    i += 2;
                }
                //#endif
                if ((mask_2 & 0x02) == 0x02)
                {// Add Altitude information
                    value = ((data[i] & 0x7F) << 8) | data[i + 1];
                    if ((data[i] & 0x80) == 0x80)
                    {
                        toRet += ";AL=-" + value;
                    }
                    else
                    {
                        toRet += ";AL=+" + value;
                    }
                    i += 2;
                }
                //#if _1W_INTERFACE == 1
                // Loads byteInfo related with the IB (iButton) extended tag (1-Wire)
                if ((mask_2 & 0x04) == 0x04)
                { // i.e. IB=8E00000053A2DF06
                    toRet += ";IB=" + new String(taipZip, i, 16);
                    i += 16;
                }
                //#endif
                // Add acceleration value
                if ((mask_2 & 0x08) == 0x08)
                {
                    toRet += ";AC=" + taipZip[i];
                    i += 1;
                }
                // Add last fix time
                if ((mask_2 & 0x10) == 0x10)
                {
                    long temp = (((((((long) data[i]) << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                    toRet += ";LF=" + temp;
//		    if ((data[i] & 0x80) == 0x80)
//		    {
//			toRet += ";LF=-1";
//		    }
//		    else
//		    {
//			value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
//			toRet += ";LF=" + value;
//		    }
                    i += 4;
                }
                // Add Jamming state
                if ((mask_2 & 0x20) == 0x20)
                {
                    if (taipZip[i] == 1)
                    {
                        toRet += ";JO";
                    }
                    i += 1;
                }
                // Add virtual hour meter
                if ((mask_2 & 0x40) == 0x40)
                {
                    value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];

                    int hours = value / 60;
                    int minutes = (int) (((value / 0.6) % 100) * 0.6);

                    toRet += ";VH=" + hours + ":" + Utils.integerF(minutes, 2);
                    i += 4;
                }
                //#if _1W_INTERFACE == 1
                //Add Total vehicle fuel used 
                if ((mask_2 & 0x80) == 0x80)
                {
                    ecuSt = (char) taipZip[i + 5];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OF=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OF=U";
                    }
                    else
                    {
                        value = (((((data[i + 1] << 8) | data[i + 2]) << 8) | data[i + 3]) << 8) | data[i + 4];

                        long fuel = (data[i] * 0x100000000L) | ((long) value & 0x00FFFFFFFFL);
                        toRet += ";OF=" + ecuSt + fuel;
                    }
                    i += 6;
                }
                //#endif
            }
            if ((groupMask & 0x04) == 0x04)
            {// Add byteInfo with mask_3
                short mask_3;
                if (i == (initPos + 2))
                {
                    mask_3 = paramMask;
                }
                else
                {
                    mask_3 = data[i];
                    i++;
                }
                System.out.println("DecodingGroup3 " + Integer.toBinaryString(mask_3 & 0xFF) + " i=" + i);
                //#if _1W_INTERFACE == 1
                if ((mask_3 & 0x01) == 0x01)
                {// Add Total vehicle distance

                    ecuSt = (char) (taipZip[i + 4] & 0x7F);
                    if (ecuSt == 'd')
                    {
                        toRet += ";OD=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OD=U";
                    }
                    else
                    {
                        long distance = (((((((long) data[i]) << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                        if ((taipZip[i + 4] & 0x80) == 0x80)
                        {//Distance greater than 2^32
                            distance |= 0x100000000L;
                        }
                        toRet += ";OD=" + ecuSt + distance;
                    }
                    i += 5;
                }
                if ((mask_3 & 0x02) == 0x02)
                {// Add Diagnostic message code

                    ecuSt = (char) (taipZip[i + 4] & 0x7F);
                    if (ecuSt == 'd')
                    {
                        toRet += ";OC=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OC=U";
                    }
                    else
                    {
                        value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                        int protocol = (data[i + 4] >> 6) & 0x02;
                        String code = Utils.ecuDecodeErrorCode(protocol, value);
                        toRet += ";OC=" + ecuSt + code;
                    }

                    i += 5;
                }
                if ((mask_3 & 0x04) == 0x04)
                {//Add Total vehicle IDLE fuel used 

                    ecuSt = (char) taipZip[i + 5];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OI=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OI=U";
                    }
                    else
                    {
                        value = (((((data[i + 1] << 8) | data[i + 2]) << 8) | data[i + 3]) << 8) | data[i + 4];

                        long fuel = (data[i] * 0x100000000L) | ((long) value & 0x00FFFFFFFFL);
                        toRet += ";OI=" + ecuSt + fuel;
                    }

                    i += 6;
                }
                if ((mask_3 & 0x08) == 0x08)
                {//Add fuel level 

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OL=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OL=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";OL=" + ecuSt + value;
                    }

                    i += 3;
                }
                if ((mask_3 & 0x10) == 0x10)
                {//Add Trip distance

                    ecuSt = (char) taipZip[i + 4];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OT=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OT=U";
                    }
                    else
                    {
                        value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                        toRet += ";OT=" + ecuSt + value;
                    }

                    i += 5;
                }
                if ((mask_3 & 0x20) == 0x20)
                {//Add Total engine usage 

                    ecuSt = (char) taipZip[i + 4];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OH=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OH=U";
                    }
                    else
                    {
                        value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                        toRet += ";OH=" + ecuSt + value;
                    }

                    i += 5;
                }
                if ((mask_3 & 0x40) == 0x40)
                {//Add Total time while engine in idle 

                    ecuSt = (char) taipZip[i + 4];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OU=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OU=U";
                    }
                    else
                    {
                        value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                        toRet += ";OU=" + ecuSt + value;
                    }

                    i += 5;
                }
                if ((mask_3 & 0x80) == 0x80)
                {//Add Instant fuel consumption 

                    ecuSt = (char) taipZip[i + 4];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OY=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OY=U";
                    }
                    else
                    {
                        value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                        toRet += ";OY=" + ecuSt + value;
                    }

                    i += 5;
                }
                //#endif

            }
            if ((groupMask & 0x08) == 0x08)
            {// Add byteInfo with mask_4
                short mask_4;
                if (i == (initPos + 2))
                {
                    mask_4 = paramMask;
                }
                else
                {
                    mask_4 = data[i];
                    i++;
                }
                System.out.println("DecodingGroup4 " + Integer.toBinaryString(mask_4 & 0xFF) + " i=" + i);
                //#if _1W_INTERFACE == 1
                if ((mask_4 & 0x01) == 0x01)
                {// Add ECU fuel level

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OX=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OX=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";OX=" + ecuSt + value;
                    }
                    i += 3;
                }
                if ((mask_4 & 0x02) == 0x02)
                {// Add rpms

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OE=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OE=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";OE=" + ecuSt + value;
                    }
                    i += 3;
                }
                if ((mask_4 & 0x04) == 0x04)
                {// Add IOS expander state 

                    if (taipZip[i] == 'U')
                    {
                        toRet += ";IE=U";
                    }
                    else
                    {
                        toRet += ";IE=" + Integer.toHexString(taipZip[i]).toUpperCase()
                                + Integer.toHexString(taipZip[i + 1]).toUpperCase()
                                + Integer.toHexString(taipZip[i + 2]).toUpperCase();
                    }
                    i += 3;
                }
                if ((mask_4 & 0x08) == 0x08)
                {// Add throttle position 

                    ecuSt = (char) taipZip[i + 1];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OA=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OA=U";
                    }
                    else
                    {
                        value = data[i];
                        toRet += ";OA=" + ecuSt + value;
                    }
                    i += 2;
                }
                //#endif
                //#ifdef _RS232_INTERFACE
                if ((mask_4 & 0x10) == 0x10)
                {// Add Photo id

                    value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];

                    toRet += ";PI=" + value;
                    i += 4;
                }
                //#endif
                if ((mask_4 & 0x20) == 0x20)
                {// Add IOS general state
                    toRet += ";IX=";
                    int index;
                    for (int j = 0; j < IOS_MAX_NBR_GROUPS; j++)
                    {
                        index = (data[i + j] >> 4) & 0x0F;
                        if (index == 0)
                        {
                            continue;
                        }

                        value = (data[i + j]) & 0x0F;
                        toRet += "" + Integer.toHexString(index).toUpperCase()
                                + Integer.toHexString(value).toUpperCase();

                    }

                    i += 10;
                }
                //#if _1W_INTERFACE == 1
                if ((mask_4 & 0x40) == 0x40)
                {// Add OBD trouble report 

                    ecuSt = (char) taipZip[i + 3];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YT=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YT=U";
                    }
                    else
                    {
                        value = (data[i + 1] << 8) | data[i + 2];
                        toRet += ";YT=" + ecuSt + (data[i] >> 7) + "," + (data[i] & 0x7F) + "," + value;

                    }
                    i += 4;
                }
                if ((mask_4 & 0x80) == 0x80)
                {// Add OBD Oxygen sensor  

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YO=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YO=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";YO=" + ecuSt + value;

                    }
                    i += 3;
                }
                //#endif
            }
            if ((groupMask & 0x10) == 0x10)
            {// Add byteInfo with mask_5
                short mask_5;
                if (i == (initPos + 2))
                {
                    mask_5 = paramMask;
                }
                else
                {
                    mask_5 = data[i];
                    i++;
                }
                System.out.println("DecodingGroup5 " + Integer.toBinaryString(mask_5 & 0xFF) + " i=" + i);
                //#if _1W_INTERFACE == 1
                if ((mask_5 & 0x01) == 0x01)
                {// Add ecu Distance with mil On

                    ecuSt = (char) taipZip[i + 4];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YD=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YD=U";
                    }
                    else
                    {
                        value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                        toRet += ";YD=" + ecuSt + value;
                    }
                    i += 5;
                }
                if ((mask_5 & 0x02) == 0x02)
                {// Efficiency

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YE=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YE=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";YE=" + ecuSt + value;
                    }
                    i += 3;
                }
                //#endif
                if ((mask_5 & 0x04) == 0x04)
                {// Current IP address 

                    toRet += ";IP=" + Utils.integerF(data[i], 3) + Utils.integerF(data[i + 1], 3)
                            + Utils.integerF(data[i + 2], 3) + Utils.integerF(data[i + 3], 3);
                    i += 4;
                }
                //#if _1W_INTERFACE == 1
                if ((mask_5 & 0x08) == 0x08)
                {// Coolant temperature

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OJ=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OJ=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        if ((data[i] & 0x80) == 0x80)
                        {//Its a negative value
                            value = value - 65536;
                        }
                        toRet += ";OJ=" + ecuSt + value;
                    }
                    i += 3;
                }
                if ((mask_5 & 0x10) == 0x10)
                {// Vehicle battery voltage

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OB=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OB=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";OB=" + ecuSt + value;
                    }
                    i += 3;
                }
                if ((mask_5 & 0x20) == 0x20)
                {// Coolant level

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OG=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OG=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";OG=" + ecuSt + value;
                    }
                    i += 3;
                }
                if ((mask_5 & 0x40) == 0x40)
                {// Transmission oil temperature
                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OK=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OK=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        if ((data[i] & 0x80) == 0x80)
                        {//Its a negative value
                            value = value - 65536;
                        }
                        toRet += ";OK=" + ecuSt + value;
                    }
                    i += 3;
                }
                if ((mask_5 & 0x80) == 0x80)
                {// Transmission liquid level

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OM=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OM=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";OM=" + ecuSt + value;
                    }
                    i += 3;
                }
                //#endif

            }
            if ((groupMask & 0x20) == 0x20)
            {// Add byteInfo with mask_6
                short mask_6;
                if (i == (initPos + 2))
                {
                    mask_6 = paramMask;
                }
                else
                {
                    mask_6 = data[i];
                    i++;
                }
                System.out.println("DecodingGroup6 " + Integer.toBinaryString(mask_6 & 0xFF) + " i=" + i);
                //#if _1W_INTERFACE == 1
                if ((mask_6 & 0x01) == 0x01)
                {// Add engine oil level
                    ecuSt = (char) taipZip[i + 1];
                    if (ecuSt == 'd')
                    {
                        toRet += ";ON=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";ON=U";
                    }
                    else
                    {
                        value = data[i];
                        toRet += ";ON=" + ecuSt + value;
                    }
                    i += 2;
                }
                if ((mask_6 & 0x02) == 0x02)
                {// Add engine oil pressure 
                    ecuSt = (char) taipZip[i + 1];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OO=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OO=U";
                    }
                    else
                    {
                        value = data[i];
                        toRet += ";OO=" + ecuSt + value;
                    }
                    i += 2;
                }
                if ((mask_6 & 0x04) == 0x04)
                {// Add engine coolant pressure 
                    ecuSt = (char) taipZip[i + 1];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OR=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OR=U";
                    }
                    else
                    {
                        value = data[i];
                        toRet += ";OR=" + ecuSt + value;
                    }
                    i += 2;
                }
                if ((mask_6 & 0x08) == 0x08)
                {// Add transmission oil pressure 
                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OV=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OV=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";OV=" + ecuSt + value;
                    }
                    i += 3;
                }
                if ((mask_6 & 0x10) == 0x10)
                {// Add Brake pedal position  
                    ecuSt = (char) taipZip[i + 1];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YB=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YB=U";
                    }
                    else
                    {
                        value = data[i];
                        toRet += ";YB=" + ecuSt + value;
                    }
                    i += 2;
                }
                if ((mask_6 & 0x20) == 0x20)
                {// Add Fan driver status   
                    ecuSt = (char) taipZip[i + 1];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YF=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YF=U";
                    }
                    else
                    {
                        value = data[i];
                        toRet += ";YF=" + ecuSt + value;
                    }
                    i += 2;
                }
                if ((mask_6 & 0x40) == 0x40)
                {// Add Hydraulic oil level  
                    ecuSt = (char) taipZip[i + 1];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YH=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YH=U";
                    }
                    else
                    {
                        value = data[i];
                        toRet += ";YH=" + ecuSt + value;
                    }
                    i += 2;
                }
                if ((mask_6 & 0x80) == 0x80)
                {// Hydraulic oil pressure 

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YI=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YI=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";YI=" + ecuSt + value;
                    }
                    i += 3;
                }
                //#endif
            }
            if ((groupMask & 0x40) == 0x40)
            {// Add byteInfo with mask_7
                short mask_7;
                if (i == (initPos + 2))
                {
                    mask_7 = paramMask;
                }
                else
                {
                    mask_7 = data[i];
                    i++;
                }
                System.out.println("DecodingGroup7 " + Integer.toBinaryString(mask_7 & 0xFF) + " i=" + i);
                //#if _1W_INTERFACE == 1
                if ((mask_7 & 0x01) == 0x01)
                {// Add Hydraulic oil temperature 
                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YJ=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YJ=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        if ((data[i] & 0x80) == 0x80)
                        {//Its a negative value
                            value = value - 65536;
                        }
                        toRet += ";YJ=" + ecuSt + value;
                    }
                    i += 3;
                }
                if ((mask_7 & 0x02) == 0x02)
                {// Add Percent torque  
                    ecuSt = (char) taipZip[i + 1];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YL=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YL=U";
                    }
                    else
                    {
                        value = data[i];
                        toRet += ";YL=" + ecuSt + value;
                    }
                    i += 2;
                }
                if ((mask_7 & 0x04) == 0x04)
                {// Add Intake manifold temperature 
                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YM=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YM=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        if ((data[i] & 0x80) == 0x80)
                        {//Its a negative value
                            value = value - 65536;
                        }
                        toRet += ";YM=" + ecuSt + value;
                    }
                    i += 3;
                }
                if ((mask_7 & 0x08) == 0x08)
                {// Add Vehicle weights 
                    ecuSt = (char) taipZip[i + 68];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OZ=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OZ=U";
                    }
                    else
                    {
                        String tag = "";
                        for (int k = 0; k < 68; k += 4)
                        {
                            value = (data[i + k]);
                            if (value != 99)
                            {//There is information in this position
                                value = ((data[i + 1 + k] << 16) | (data[i + 2 + k] << 8)) | (data[i + 3 + k]);

                                if (k == (13 * 4) || k == (14 * 4))
                                {//Can be a special weight
                                    if ((value & 0x800000) == 0x800000)
                                    {
                                        tag += "," + Utils.integerF((k / 4) + 85, 2) + (value & 0x7FFFFF);
                                        continue;
                                    }
                                }
                                tag += "," + Utils.integerF((k / 4), 2) + value;
                            }
                        }
                        if (tag.length() > 1)
                        {
                            tag = tag.substring(1);
                        }
                        else if (ecuSt != 'D')
                        {
                            ecuSt = 'U';
                        }

                        toRet += ";OZ=" + ecuSt + tag;
                    }
                    i += 69;
                }
                if ((mask_7 & 0x10) == 0x10)
                {// Add tire temperatures
                    ecuSt = (char) taipZip[i + 174];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YN=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YN=U";
                    }
                    else
                    {
                        int location;
                        String tireInfo = "";
                        for (int k = 0; k < 174; k += 3)
                        {
                            location = (data[i + k]);
                            if (location == 0x0F)
                            {
                                break;
                            }

                            //There is information in this position
                            value = (data[i + 1 + k] << 8) | (data[i + 2 + k]);
                            if ((data[i + 1 + k] & 0x80) == 0x80)
                            {//Its a negative value
                                value = value - 65536;
                            }
                            tireInfo += "," + Utils.integerF(location, 3) + value;

                        }
                        if (tireInfo.length() > 1)
                        {
                            tireInfo = tireInfo.substring(1);
                        }
                        else if (ecuSt != 'D')
                        {
                            ecuSt = 'U';
                        }

                        toRet += ";YN=" + ecuSt + tireInfo;
                    }
                    i += 175;
                }
                if ((mask_7 & 0x20) == 0x20)
                {// Add ECU Quality

                    value = ((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]);
                    toRet += ";EQ=" + value + "," + data[i + 3];
                    i += 4;
                }
                //#endif
                if ((mask_7 & 0x40) == 0x40)
                {// Add Time Counter value
                    do
                    {
                        value = ((((((data[i]) << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                        toRet += ";TC" + Utils.integerF(data[i + 4] & 0x1F, 2) + "=" + value;
                        i += 5;
                    }
                    while ((data[i - 1] & 0x80) == 0x80);
                }
                if ((mask_7 & 0x80) == 0x80)
                {// Acnowledge tag
                    value = ((((data[i + 1] << 8) | data[i + 2]) << 8) | data[i + 3]);
                    toRet += ";SA=" + data[i] + "," + value;
                    i += 4;
                }
            }
            if ((groupMask & 0x80) == 0x80)
            {
                short mask_8;
                if (i == (initPos + 2))
                {
                    mask_8 = paramMask;
                }
                else
                {
                    mask_8 = data[i];
                    i++;
                }
                System.out.println("DecodingGroup8 " + Integer.toBinaryString(mask_8 & 0xFF) + " i=" + i);
                if ((mask_8 & 0x01) == 0x01)
                {
                    char regionType = (char) taipZip[i + 1];
                    //Check if the unit is inside of the region  
                    if ((data[i] & 0x80) == 0x80)
                    {
                        toRet += ";RE=I" + regionType + Utils.integerF(data[i] & 0x7F, 2);
                    }
                    else
                    {
                        toRet += ";RE=O" + regionType + Utils.integerF(data[i] & 0x7F, 2);
                    }

                    i += 2;
                }
                //#if _1W_INTERFACE == 1
                if ((mask_8 & 0x02) == 0x02)
                {// Engine oil temperature

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";YK=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";YK=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        if ((data[i] & 0x80) == 0x80)
                        {//Its a negative value
                            value = value - 65536;
                        }
                        toRet += ";YK=" + ecuSt + value;
                    }
                    i += 3;
                }
                if ((mask_8 & 0x04) == 0x04)
                {// Vehicle speed

                    ecuSt = (char) taipZip[i + 2];
                    if (ecuSt == 'd')
                    {
                        toRet += ";OS=D";
                    }
                    else if (ecuSt == 'U')
                    {
                        toRet += ";OS=U";
                    }
                    else
                    {
                        value = (data[i] << 8) | data[i + 1];
                        toRet += ";OS=" + ecuSt + value;
                    }
                    i += 3;
                }
                //#endif
                if ((mask_8 & 0x08) == 0x08)
                {// Pulse Counter
                    value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                    toRet += ";PC=" + value;
                    i += 4;
                }
                if ((mask_8 & 0x10) == 0x10)
                {// Trigger Information
                    toRet += ";TI=" + data[i] + "," + new String(taipZip, i + 1, 3) + ",";
                    value = (((((data[i + 4] << 8) | data[i + 5]) << 8) | data[i + 6]) << 8) | data[i + 7];
                    toRet += value + ",";
                    value = (((((data[i + 8] << 8) | data[i + 9]) << 8) | data[i + 10]) << 8) | data[i + 11];
                    toRet += value;
                    i += 12;
                }
                //#if _1W_INTERFACE == 1
                if ((mask_8 & 0x20) == 0x20)
                {// PGN_PID
                    int size = data[i];
                    for (int k = 1; k < size; k += 11)
                    {
                        toRet += ";PG" + ((data[i + k] >> 4) & 0x0F) + "=";
                        toRet += data[i + k] & 0x01;
                        byte[] temp = new byte[10];
                        System.arraycopy(taipZip, i + k + 1, temp, 0, 10);
                        toRet += Utils.toHexStringUpperCase(temp);
                    }
                    i += 56;
                }
                //#endif
                if ((mask_8 & 0x40) == 0x40)
                {// Ignition counter
                    value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                    toRet += ";CE=" + value;
                    i += 4;
                }
                if ((mask_8 & 0x80) == 0x80)
                {// Idle counter
                    value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                    toRet += ";CL=" + value;
                    i += 4;
                }
            }

            if (data.length >= (i + 3))
            {//There is more information
                int newGroup = data[i++];
                System.out.println("extGroup " + Integer.toBinaryString(newGroup & 0xFF));
                if ((newGroup & 0x01) == 0x01)
                {
                    short mask_9 = data[i++];
                    System.out.println("DecodingGroup9 " + Integer.toBinaryString(mask_9 & 0xFF) + " i=" + i);
                    if ((mask_9 & 0x01) == 0x01)
                    {// Over speed counter
                        value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                        toRet += ";CS=" + value;
                        i += 4;
                    }
                    //#if _1W_INTERFACE == 1
                    if ((mask_9 & 0x02) == 0x02)
                    {// Over RPM counter
                        value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                        toRet += ";CR=" + value;
                        i += 4;
                    }
                    if ((mask_9 & 0x04) == 0x04)
                    {// Ecu Service distance 
                        ecuSt = (char) taipZip[i + 3];
                        if (ecuSt == 'd')
                        {
                            toRet += ";OW=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";OW=U";
                        }
                        else
                        {
                            value = (((data[i] << 8) | data[i + 1]) << 8) | data[i + 2];
                            if ((data[i] & 0x80) == 0x80)
                            {//Its a negative value
                                value = value - 0x1000000;
                            }
                            toRet += ";OW=" + ecuSt + value;
                        }
                        i += 4;
                    }
                    if ((mask_9 & 0x08) == 0x08)
                    {// Ibutton state
                        toRet += ";IS=" + taipZip[i];
                        i++;
                        for (int j = 7; j >= 0; j--)
                        {
                            toRet += Integer.toString((taipZip[i + j] & 0xFF) + 0x100, 16).substring(1).toUpperCase();
                        }
                        i += 8;
                    }
                    //#endif
                    //#ifdef _RS232_INTERFACE
                    if ((mask_9 & 0x10) == 0x10)
                    {// Finger print information
                        int state = (data[i] >> 7) & 0x01;
                        int length = (data[i] & 0x7F) / 2;

                        toRet += ";FP=" + state + ",";
                        if (length == 0)
                        {//No finger print read or there is an error
                            toRet += "*,";
                        }
                        else
                        {
                            String id = "";
                            for (int j = 1; j <= length; j++)
                            {
                                id += Integer.toString((data[i + j] & 0xff) + 0x100, 16).substring(1);
                            }
                            toRet += id.toUpperCase() + ",";
                        }
                        toRet += Integer.toHexString(data[i + 12]).toUpperCase();
                        i += 13;
                    }
                    if ((mask_9 & 0x20) == 0x20)
                    {// Thechnoton Fuel Frequency, temperature and level
                        value = ((data[i + 1] << 8) | data[i + 2]);
                        toRet += ";FF=" + data[i] + "," + value;
                        toRet += "," + taipZip[i + 3];
                        value = ((data[i + 4] << 8) | data[i + 5]);
                        toRet += "," + value;
                        i += 6;
                    }
                    if ((mask_9 & 0x40) == 0x40)
                    {// Add Photo ids

                        for (int k = 0; k < MAX_NBR_OF_SERIAL_CAMERAS; k++)
                        {
                            value = (((((data[i + (k * 4)] << 8) | data[(i + 1) + (k * 4)]) << 8) | data[(i + 2)
                                    + (k * 4)]) << 8)
                                    | data[(i + 3) + (k * 4)];

                            if (value > 0)
                            {
                                int port = value / 10000000;
                                toRet += ";PS" + Utils.integerF(port, 2) + "=" + value;
                            }
                        }
                        i += 40;
                    }
                    //#endif
                    if ((mask_9 & 0x80) == 0x80)
                    {// Add Communication counters					
                        value = (((data[i] << 8) | data[i + 1]) << 8) | data[i + 2];
                        toRet += ";CC=" + value;
                        value = (((data[i + 3] << 8) | data[i + 4]) << 8) | data[i + 5];
                        toRet += "," + value;
                        value = (((data[i + 6] << 8) | data[i + 7]) << 8) | data[i + 8];
                        toRet += "," + value;
                        value = (((data[i + 9] << 8) | data[i + 10]) << 8) | data[i + 11];
                        toRet += "," + value;
                        long temp = (((((((((long) data[i + 12]) << 8) | data[i + 13]) << 8) | data[i + 14]) << 8) | data[i + 15]) << 8)
                                | data[i + 16];
                        toRet += "," + temp;
                        temp = (((((((((long) data[i + 17]) << 8) | data[i + 18]) << 8) | data[i + 19]) << 8) | data[i + 20]) << 8)
                                | data[i + 21];
                        toRet += "," + temp;
                        value = (((data[i + 22] << 8) | data[i + 23]) << 8) | data[i + 24];
                        toRet += "," + value;
                        value = (((data[i + 25] << 8) | data[i + 26]) << 8) | data[i + 27];
                        toRet += "," + value;

                        i += 34;
                    }
                }
                if ((newGroup & 0x02) == 0x02)
                {
                    short mask_10 = data[i++];
                    System.out.println("DecodingGroup10 " + Integer.toBinaryString(mask_10 & 0xFF) + " i=" + i);
                    if ((mask_10 & 0x01) == 0x01)
                    {// Add VO new format
                        long vo = (((((((((long) data[i]) << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3]) << 8)
                                | data[i + 4];
                        toRet += ";VO=" + vo;
                        i += 5;
                    }
//		    if ((mask_10 & 0x02) == 0x02)
//		    {// Add cell monitoring
//			toRet += ";CI=";
//			for (int j = 0; j < 70; j += 2)
//			{
//			    value = (data[i + j] << 8) | data[i + 1 + j];
//			    if (j == 0)
//			    {
//				if (value == 0)
//				{//There is no cell information 
//				    toRet += "U";
//				    break;
//				}
//				toRet += value;
//				continue;
//			    }
//			    else if (j % 10 == 0 && value == 0)
//			    {//There is no more information 
//				break;
//			    }
//			    toRet += "," + value;
//			}
//			i += 70;
//		    }
                    if ((mask_10 & 0x02) == 0x02)
                    {// Add serving cell
                        toRet += ";SC=";
                        if (data[i] == 0)
                        {
                            toRet += "U";
                        }
                        else
                        {
                            toRet += data[i] + "G,";
                            value = (data[i + 1] << 8) | data[i + 2];
                            toRet += value + ",";
                            value = (data[i + 3] << 8) | data[i + 4];
                            toRet += value + ",";
                            value = (data[i + 5] << 8) | data[i + 6];
                            toRet += Integer.toHexString(value).toUpperCase() + ",";
                            value = (((((data[i + 7] << 8) | data[i + 8]) << 8) | data[i + 9]) << 8) | data[i + 10];
                            toRet += Integer.toHexString(value).toUpperCase() + ",";
                            toRet += data[i + 11];
                        }
                        i += 14;
                    }
                    //#if _1W_INTERFACE == 1
                    if ((mask_10 & 0x04) == 0x04)
                    {// Mobileye parameters
                        toRet += ";ME=";
                        if ((data[i] & 0x80) == 0x80)
                        {
                            toRet += "1";
                            byte[] temp = new byte[8];
                            System.arraycopy(taipZip, i, temp, 0, temp.length);
                            toRet += Utils.mobileyeDecode700(temp);
                            temp = new byte[3];
                            System.arraycopy(taipZip, i + 8, temp, 0, temp.length);
                            toRet += Utils.mobileyeDecode760(temp);
                        }
                        else
                        {
                            toRet += "0";
                        }
                        i += 11;
                    }
                    if ((mask_10 & 0x08) == 0x08)
                    {// Total revolutions  
                        long tr = (((((((((long) data[i]) << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3]) << 8)
                                | data[i + 4];
                        toRet += ";TR=" + tr;
                        i += 5;
                    }
                    if ((mask_10 & 0x10) == 0x10)
                    {// Ambient air temperature
                        ecuSt = (char) taipZip[i + 2];
                        if (ecuSt == 'd')
                        {
                            toRet += ";YA=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";YA=U";
                        }
                        else
                        {
                            value = (data[i] << 8) | data[i + 1];
                            if ((data[i] & 0x80) == 0x80)
                            {//Its a negative value
                                value = value - 65536;
                            }
                            toRet += ";YA=" + ecuSt + value;
                        }
                        i += 3;
                    }
                    if ((mask_10 & 0x20) == 0x20)
                    {// Driver's Demand Engine - Percent Torque
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";YU=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";YU=U";
                        }
                        else
                        {
                            value = data[i];
                            toRet += ";YU=" + ecuSt + value;
                        }
                        i += 2;
                    }
                    if ((mask_10 & 0x40) == 0x40)
                    {// Clutch Switch
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";YC=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";YC=U";
                        }
                        else
                        {
                            value = data[i];
                            toRet += ";YC=" + ecuSt + value;
                        }
                        i += 2;
                    }
                    if ((mask_10 & 0x80) == 0x80)
                    {// OBD2 Engine Load
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";YW=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";YW=U";
                        }
                        else
                        {
                            value = data[i];
                            toRet += ";YW=" + ecuSt + value;
                        }
                        i += 2;
                    }
                    //#endif
                }
                if ((newGroup & 0x04) == 0x04)
                {
                    short mask_11 = data[i++];
                    System.out.println("DecodingGroup11 " + Integer.toBinaryString(mask_11 & 0xFF) + " i=" + i);
                    //#if _1W_INTERFACE == 1
                    if ((mask_11 & 0x01) == 0x01)
                    {// OBD2 Fuel Pressure
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";YG=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";YG=U";
                        }
                        else
                        {
                            value = data[i];
                            toRet += ";YG=" + ecuSt + value;
                        }
                        i += 2;
                    }
                    if ((mask_11 & 0x02) == 0x02)
                    {// OBD2 Intake manifold absolute pressure
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";YP=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";YP=U";
                        }
                        else
                        {
                            value = data[i];
                            toRet += ";YP=" + ecuSt + value;
                        }
                        i += 2;
                    }
                    if ((mask_11 & 0x04) == 0x04)
                    {// OBD2 Intake air temperature
                        ecuSt = (char) taipZip[i + 2];
                        if (ecuSt == 'd')
                        {
                            toRet += ";YX=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";YX=U";
                        }
                        else
                        {
                            value = (data[i] << 8) | data[i + 1];
                            if ((data[i] & 0x80) == 0x80)
                            {//Its a negative value
                                value = value - 65536;
                            }
                            toRet += ";YX=" + ecuSt + value;
                        }
                        i += 3;
                    }
                    if ((mask_11 & 0x08) == 0x08)
                    {// OBD2 Time since engine start
                        ecuSt = (char) taipZip[i + 2];
                        if (ecuSt == 'd')
                        {
                            toRet += ";YS=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";YS=U";
                        }
                        else
                        {
                            value = (data[i] << 8) | data[i + 1];
                            toRet += ";YS=" + ecuSt + value;
                        }
                        i += 3;
                    }
                    if ((mask_11 & 0x10) == 0x10)
                    {// OBD2 Barometric pressure
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";YY=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";YY=U";
                        }
                        else
                        {
                            value = data[i];
                            toRet += ";YY=" + ecuSt + value;
                        }
                        i += 2;
                    }
                    if ((mask_11 & 0x20) == 0x20)
                    {// PTO Status
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";OP=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";OP=U";
                        }
                        else
                        {
                            value = data[i];
                            toRet += ";OP=" + ecuSt + value;
                        }
                        i += 2;
                    }
                    if ((mask_11 & 0x40) == 0x40)
                    {// Cruise control Status
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";OQ=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";OQ=U";
                        }
                        else
                        {
                            value = data[i];
                            toRet += ";OQ=" + ecuSt + value;
                        }
                        i += 2;
                    }
                    if ((mask_11 & 0x80) == 0x80)
                    {// OBD2 Absolute load value
                        ecuSt = (char) taipZip[i + 2];
                        if (ecuSt == 'd')
                        {
                            toRet += ";YV=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";YV=U";
                        }
                        else
                        {
                            value = (data[i] << 8) | data[i + 1];
                            toRet += ";YV=" + ecuSt + value;
                        }
                        i += 3;
                    }
                    //#endif
                }
                if ((newGroup & 0x08) == 0x08)
                {
                    short mask_12 = data[i++];
                    System.out.println("DecodingGroup12 " + Integer.toBinaryString(mask_12 & 0xFF) + " i=" + i);
                    //#if _1W_INTERFACE == 1
                    if ((mask_12 & 0x01) == 0x01)
                    {// OBD2 Time run by the engine since MIL is activated
                        ecuSt = (char) taipZip[i + 2];
                        if (ecuSt == 'd')
                        {
                            toRet += ";YZ=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";YZ=U";
                        }
                        else
                        {
                            value = (data[i] << 8) | data[i + 1];
                            toRet += ";YZ=" + ecuSt + value;
                        }
                        i += 3;
                    }
                    if ((mask_12 & 0x02) == 0x02)
                    {//  OBD2 percent remaining life for the hybrid battery pack.
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZB=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZB=U";
                        }
                        else
                        {
                            value = data[i];
                            toRet += ";ZB=" + ecuSt + value;
                        }
                        i += 2;
                    }
                    if ((mask_12 & 0x04) == 0x04)
                    {//  OBD2 Time run by the engine since DTCs codes were cleared by an external tool
                        ecuSt = (char) taipZip[i + 2];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZD=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZD=U";
                        }
                        else
                        {
                            value = (data[i] << 8) | data[i + 1];
                            toRet += ";ZD=" + ecuSt + value;
                        }
                        i += 3;
                    }
                    if ((mask_12 & 0x08) == 0x08)
                    {// OBD2 Auxiliary Inputs / Outputs
                        ecuSt = (char) taipZip[i + 2];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZP=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZP=U";
                        }
                        else
                        {
                            value = (data[i] << 8) | data[i + 1];
                            toRet += ";ZP=" + ecuSt + value;
                        }
                        i += 3;
                    }
                    //#endif 

                    if ((mask_12 & 0x10) == 0x10)
                    {// Data Logger event
                        long epoch = Utils.byteArrayToLong(taipZip, i, 8);
                        toRet += ";DL=" + epoch;
                        for (int j = i + 8; j < (i + 23) && taipZip[j] != 0; j += 3)
                        {
                            toRet += ',';
                            toRet += (char) (taipZip[j]);
                            toRet += (char) (taipZip[j + 1]);
                            toRet += (char) (taipZip[j + 2]);
                        }
                        i += 23;
                    }
                    if ((mask_12 & 0x20) == 0x20)
                    {// Acceleration values
                        toRet += ";AV=" + data[i];
                        i += 1;
                    }

                    //#if _1W_INTERFACE == 1
                    if ((mask_12 & 0x40) == 0x40)
                    {// Ecu multiple error codes
                        System.out.println("DecodingZC");
                        ecuSt = (char) (taipZip[i + 42]);
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZC=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZC=U";
                        }
                        else
                        {
                            //Number of errors
                            int errors = (data[i] << 8) | data[i + 1];
                            value = (((((data[i + 2] << 8) | data[i + 3]) << 8) | data[i + 4]) << 8) | data[i + 5];
                            if (value == 0 || errors == 0)
                            {
                                toRet += ";ZC=" + ecuSt + "0$0,0,0,0";
                            }
                            else
                            {
                                //Adding first code 
                                toRet += ";ZC=" + ecuSt + errors + "$" + Utils.ecuDecodeErrorCode(1, value);
                                //Adding 9 codes -> 4 bytes for everyone 
                                for (int j = i + 6; j < ((10 * 4) + i); j += 4)
                                {
                                    value = (((((data[j] << 8) | data[j + 1]) << 8) | data[j + 2]) << 8) | data[j + 3];
                                    if (value == 0)
                                    {
                                        break;
                                    }
                                    toRet += "$" + Utils.ecuDecodeErrorCode(1, value);
                                }
                            }
                        }
                        i += 43;
                    }
                    if ((mask_12 & 0x80) == 0x80)
                    {// Bluetooth IOT TAG
                        toRet += ";BT=";
                        if (taipZip[i] != 0)
                        {
                            //#if _BLUETOOTH == 1
                            byte[] rawAdd = new byte[6];
                            System.arraycopy(taipZip, i, rawAdd, 0, 6);
                            BDAddr add = new BDAddr(rawAdd);
                            toRet += add.toString();
                            //Temperature
                            toRet += ",";
                            value = (data[i + 6] << 8) | data[i + 7];
                            if ((data[i + 6] & 0x80) == 0x80)
                            {//Its a negative value
                                value = value - 65536;
                            }
                            if (value != -999)
                            {
                                toRet += value;
                            }
                            // Relative Humidity
                            toRet += ",";
                            if ((data[i + 8] & 0x80) != 0x80)
                            {
                                toRet += data[i + 8];
                            }
                            // Light
                            toRet += ",";
                            if ((data[i + 9] & 0x80) != 0x80)
                            {
                                toRet += data[i + 9];
                            }
                            //Batt
                            toRet += ",";
                            if ((data[i + 10] & 0x80) != 0x80)
                            {
                                toRet += data[i + 10];
                            }
                            // Motion
                            toRet += ",";
                            if ((data[i + 11] & 0x02) != 0x02)
                            {
                                toRet += data[i + 11] & 0x01;
                            }
                            // FreeFall
                            toRet += ",";
                            if ((data[i + 11] & 0x08) != 0x08)
                            {
                                toRet += (data[i + 12] >> 2) & 0x01;
                            }
                            // Impact
                            toRet += ",";
                            if ((data[i + 11] & 0x20) != 0x20)
                            {
                                toRet += (data[i + 13] >> 4) & 0x01;
                            }
                            // Button
                            toRet += ",";
                            if ((data[i + 11] & 0x80) != 0x80)
                            {
                                toRet += (data[i + 11] >> 6) & 0x01;
                            }
                            // ReedSw
                            toRet += ",";
                            if ((data[i + 12] & 0x02) != 0x02)
                            {
                                toRet += data[i + 12] & 0x01;
                            }
                            toRet += "," + (char) data[i + 13];
                            // Presence
                            toRet += "," + data[i + 14];
                            // Reference time configured
                            toRet += "," + data[i + 15];
                            //#else
//#                             toRet += 'U';
                            //#endif  

                        }
                        else
                        {
                            toRet += 'U';
                        }
                        i += 19;
                    }
                    //#endif
                }
                if ((newGroup & 0x10) == 0x10)
                {
                    short mask_13 = data[i++];
                    System.out.println("DecodingGroup13 " + Integer.toBinaryString(mask_13 & 0xFF) + " i=" + i);
                    //#if _1W_INTERFACE == 1
                    if ((mask_13 & 0x01) == 0x01)
                    {// ECU Diesel exhaust fluid level 
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZL=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZL=U";
                        }
                        else
                        {
                            value = data[i];
                            toRet += ";ZL=" + ecuSt + value;
                        }
                        i += 2;
                    }
                    if ((mask_13 & 0x02) == 0x02)
                    {// ECU Diesel exhaust fluid level temperature
                        ecuSt = (char) taipZip[i + 2];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZT=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZT=U";
                        }
                        else
                        {
                            value = (data[i] << 8) | data[i + 1];
                            if ((data[i] & 0x80) == 0x80)
                            {//Its a negative value
                                value = value - 65536;
                            }
                            toRet += ";ZT=" + ecuSt + value;
                        }
                        i += 3;
                    }
                    if ((mask_13 & 0x04) == 0x04)
                    {// ECU Diesel exhaust fluid consumption 
                        ecuSt = (char) taipZip[i + 4];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZU=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZU=U";
                        }
                        else
                        {
                            value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                            toRet += ";ZU=" + ecuSt + value;
                        }
                        i += 5;
                    }
                    if ((mask_13 & 0x08) == 0x08)
                    {// ECU Vehicle identification number
                        ecuSt = (char) taipZip[i + 17];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZV=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZV=U";
                        }
                        else
                        {
                            toRet += ";ZV=" + ecuSt;
                            for (int j = 0; j < 17; j++)
                            {
                                if (data[i + j] == 0)
                                {
                                    break;
                                }
                                toRet += (char) data[i + j];
                            }
                        }
                        i += 18;
                    }
                    if ((mask_13 & 0x10) == 0x10)
                    {// ECU Total MAF used
                        ecuSt = (char) taipZip[i + 4];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZM=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZM=U";
                        }
                        else
                        {
                            value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                            toRet += ";ZM=" + ecuSt + value;
                        }
                        i += 5;
                    }
                    if ((mask_13 & 0x20) == 0x20)
                    {// ECU Fuel Type
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZF=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZF=U";
                        }
                        else
                        {
                            value = data[i];
                            toRet += ";ZF=" + ecuSt + value;
                        }
                        i += 2;
                    }
                    if ((mask_13 & 0x40) == 0x40)
                    {// Generic 1-wire device                                      
                        long temp = Utils.byteArrayToLong(taipZip, i, 6);
                        temp |= 0x1000000000000L;
                        toRet += ";WG=" + Long.toString(temp, 16).substring(1).toUpperCase();
                        i += 6;
                    }
                    if ((mask_13 & 0x80) == 0x80)
                    {// Bluetooth Driver TAG
                        toRet += ";BD=";
                        if (taipZip[i] != 0)
                        {
                            //#if _BLUETOOTH == 1
                            byte[] rawAdd = new byte[6];
                            System.arraycopy(taipZip, i, rawAdd, 0, 6);
                            BDAddr add = new BDAddr(rawAdd);
                            toRet += data[i + 6] + add.toString();
                            //#else
//#                             toRet += 'U';
                            //#endif  

                        }
                        else
                        {
                            toRet += 'U';
                        }
                        i += 7;
                    }
                    //#endif
                }
                if ((newGroup & 0x20) == 0x20)
                {
                    short mask_14 = data[i++];
                    System.out.println("DecodingGroup14 " + Integer.toBinaryString(mask_14 & 0xFF) + " i=" + i);
                    if ((mask_14 & 0x01) == 0x01)
                    {//RF Id reader
                        long id = Utils.byteArrayToLong(taipZip, i, 8);
                        if (id == 0)
                        {
                            toRet += ";RI=U";
                        }
                        else
                        {
                            toRet += ";RI=" + id;
                        }
                        i += 8;
                    }
                    if ((mask_14 & 0x02) == 0x02)
                    {// Accelerometer parameters
                        //Motion
                        toRet += ";AP=" + data[i] + ",";
                        //Instant Acceleration
                        toRet += ((((((data[i + 22] << 8) | data[i + 21]) << 8) | data[i + 20]) << 8) | data[i + 19]) + ",";
                        //X reference angle
                        toRet += ((data[i + 2] << 8) | data[i + 1]) + ",";
                        //Y reference angle
                        toRet += ((data[i + 4] << 8) | data[i + 3]) + ",";
                        //Z reference angle
                        toRet += ((data[i + 6] << 8) | data[i + 5]) + ",";
                        //X current angle
                        toRet += ((data[i + 8] << 8) | data[i + 7]) + ",";
                        //Y current angle
                        toRet += ((data[i + 10] << 8) | data[i + 9]) + ",";
                        //Z current angle
                        toRet += ((data[i + 12] << 8) | data[i + 11]);
//			//Head X
//			toRet += ((data[i + 14] << 8) | data[i + 13]) + ",";
//			//Head Y
//			toRet += ((data[i + 16] << 8) | data[i + 15]) + ",";
//			//Head Z
//			toRet += ((data[i + 18] << 8) | data[i + 17]) + ",";
//			//Instant Head
//			toRet += ((((((data[i + 26] << 8) | data[i + 25]) << 8) | data[i + 24]) << 8) | data[i + 23]) + ",";
                        i += 32;
                    }
                    //#if _1W_INTERFACE == 1
                    if ((mask_14 & 0x04) == 0x04)
                    {// Add tire pressure
                        ecuSt = (char) taipZip[i + 174];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZN=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZN=U";
                        }
                        else
                        {
                            int location;
                            String tireInfo = "";
                            for (int k = 0; k < 174; k += 3)
                            {
                                location = (data[i + k]);
                                if (location == 0x0F)
                                {
                                    break;
                                }

                                //There is information in this position
                                value = (data[i + 1 + k] << 8) | (data[i + 2 + k]);
                                tireInfo += "," + Utils.integerF(location, 3) + value;
                            }
                            if (tireInfo.length() > 1)
                            {
                                tireInfo = tireInfo.substring(1);
                            }
                            else if (ecuSt != 'D')
                            {
                                ecuSt = 'U';
                            }

                            toRet += ";ZN=" + ecuSt + tireInfo;
                        }
                        i += 175;
                    }
                    if ((mask_14 & 0x08) == 0x08)
                    {//Continental Tire: Tpms configuration and current state 
                        ecuSt = (char) taipZip[i + 6];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZI=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZI=U";
                        }
                        else
                        {
                            //System id
                            toRet += ";ZI=" + ecuSt + (data[i] & 0x03);
                            //System State
                            toRet += "" + ((data[i] >> 2) & 0x03);
                            if (data[i + 1] != 0)
                            {
                                //Number of axles
                                toRet += Utils.integerF((data[i + 1] >> 2) & 0x1F, 2);
                                //Number of ttms
                                toRet += Utils.integerF(data[i + 2] & 0xFF, 2);
                            }
                            //Location vs sensor id
                            int location;
                            String tireInfo = "";
                            for (int k = 7; k < 247; k += 5)
                            {
                                location = (data[i + k]);
                                if (location == 0xFF)
                                {
                                    break;
                                }

                                //There is information in this position
                                value = Utils.byteArrayToInt(taipZip, i + 1 + k, 4);
                                tireInfo += "," + Utils.integerF(location, 3) + value;
                            }
                            toRet += tireInfo;

                        }
                        i += 247;
                    }
                    if ((mask_14 & 0x10) == 0x10)
                    {//Continental Tire: Tires under warning state
                        ecuSt = (char) taipZip[i + 48];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZW=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZW=U";
                        }
                        else
                        {

                            int location;
                            String tireInfo = "";
                            for (int k = 0; k < 49; k += 2)
                            {
                                location = (data[i + k]);
                                if (location == 0x0F)
                                {
                                    break;
                                }

                                //There is information in this position
                                value = (data[i + 1 + k]);
                                if (value != 0)
                                {
                                    tireInfo += "," + Utils.integerF(location, 3);
                                    //Alarm + warnings
                                    tireInfo += value & 0x0F;
                                    //TTM defective
                                    tireInfo += (value >> 4) & 0x01;
                                    //TTM detection
                                    tireInfo += (value >> 5) & 0x03;
                                    //Battery
                                    tireInfo += (value >> 7) & 0x01;
                                }
                            }
                            if (tireInfo.length() > 1)
                            {
                                tireInfo = tireInfo.substring(1);
                            }
                            else if (ecuSt != 'D')
                            {
                                ecuSt = 'U';
                            }

                            toRet += ";ZW=" + ecuSt + tireInfo;
                        }
                        i += 49;
                    }
                    //#endif
                    if ((mask_14 & 0x20) == 0x20)
                    {// Vehicle inclination			
                        toRet += ";VI=" + data[i];
                        i += 1;
                    }
                    if ((mask_14 & 0x40) == 0x40)
                    {// Axle pressure meter
                        if (data[i] == 0xFF && data[i + 1] == 0xFF)
                        {
                            toRet += ";PM=U";
                        }
                        else
                        {
                            String values = "";
                            for (int j = 0; j < 12; j++)
                            {
                                value = (data[i + (j * 2) + 1] << 8) | (data[i + (j * 2)]);
                                values += "," + value;
                            }
                            toRet += ";PM=" + values.substring(1);
                        }
                        i += 24;
                    }
                    if ((mask_14 & 0x80) == 0x80)
                    {// Axle pressure meter
                        if (data[i] == 0xFF && data[i + 1] == 0xFF)
                        {
                            toRet += ";AW=U";
                        }
                        else
                        {
                            String values = "";
                            for (int j = 0; j < 16; j++)
                            {
                                if (data[i + (j * 2) + 1] == 0xFF && data[i + (j * 2)] == 0xFF)
                                {
                                    break;
                                }
                                value = (data[i + (j * 2) + 1] << 8) | (data[i + (j * 2)]);
                                {
                                    values += "," + value;
                                }
                            }
                            toRet += ";AW=" + values.substring(1);
                        }
                        i += 32;
                    }
                }
                if ((newGroup & 0x40) == 0x40)
                {
                    short mask_15 = data[i++];
                    System.out.println("DecodingGroup15 " + Integer.toBinaryString(mask_15 & 0xFF) + " i=" + i);
                    if ((mask_15 & 0x01) == 0x01)
                    {//Fuel sensor with serial expander
                        for (int j = 0; j < MAX_NBR_OF_SENSORS - 1; j++)
                        {
                            if (data[i + (j * 6)] == 0xFF)
                            {
                                continue;
                            }
                            value = ((data[i + 1 + (j * 6)] << 8) | data[i + 2 + (j * 6)]);
                            toRet += ";FE" + (j + 1) + "=" + data[i + (j * 6)] + "," + value;
                            toRet += "," + taipZip[i + 3 + (j * 6)];
                            value = ((data[i + 4 + (j * 6)] << 8) | data[i + 5 + (j * 6)]);
                            toRet += "," + value;
                        }
                        i += 18;
                    }
                    //#if _BLUETOOTH == 1
                    if ((mask_15 & 0x02) == 0x02)
                    {//Bluetooth tag data logger
                        toRet += ";BS=";
                        String samples = "";
                        for (int k = 0; k < 18; k += 6)
                        {
                            byte[] chunk = new byte[6];
                            System.arraycopy(taipZip, i + k + 6, chunk, 0, chunk.length);
                            String temp = Utils.toHexStringUpperCase(chunk);
                            if (temp.equals("000000000000"))
                            {
                                if (k == 0)
                                {
                                    toRet += 'U';
                                }
                                break;
                            }
                            samples += "," + temp + "0000";
                        }
                        if (samples.length() > 0)
                        {
                            byte[] rawAdd = new byte[6];
                            System.arraycopy(taipZip, i, rawAdd, 0, 6);
                            BDAddr add = new BDAddr(rawAdd);
                            toRet += add.toString();
                            //Samples
                            toRet += samples;
                        }
                        i += 24;
                    }
                    //#endif
                    if ((mask_15 & 0x04) == 0x04)
                    {//Utrasonic fuel sensor
                        toRet += ";US=";
                        if (taipZip[i + 4] == 0 && taipZip[i + 5] == 0)
                        {//No valid data
                            toRet += "U";
                        }
                        else
                        {
                            for (int k = 0; k < 14; k += 2)
                            {
                                byte[] chunk = new byte[2];
                                System.arraycopy(taipZip, i + k, chunk, 0, chunk.length);
                                String temp = Utils.toHexStringUpperCase(chunk);
                                if (k == 0)
                                {
                                    toRet += temp;
                                    continue;
                                }
                                if (k == 2)
                                {//Minutes
                                    temp = temp.substring(2);
                                }
                                toRet += "," + temp;
                            }
                        }
                        i += 14;
                    }
                    if ((mask_15 & 0x08) == 0x08)
                    {//Movon driver assistance 
                        if (taipZip[i] == (byte) 0xFF)
                        {
                            toRet += ";MV=U";
                        }
                        else
                        {
                            byte[] movonData = new byte[20];
                            System.arraycopy(taipZip, i + 1, movonData, 0, movonData.length);
                            toRet += ";MV=" + taipZip[i] + Utils.movonGetDataDecoded(movonData);
                        }
                        i += 30;
                    }
                    if ((mask_15 & 0x10) == 0x10)
                    {//ECU general indicators
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZS=D";
                        }
                        else if (ecuSt == 'u')
                        {
                            toRet += ";ZS=U";
                        }
                        else
                        {
                            toRet += ";ZS=";
                            for (int k = 0; k < 11; k++)
                            {
                                ecuSt = (char) taipZip[(k * 2) + i + 1];
                                if (ecuSt != 'U')
                                {
                                    value = (data[(k * 2) + i]) & 0xFF;
                                    toRet += "" + ecuSt + value;
                                }
                                if (k + 1 < 11)
                                {
                                    toRet += ',';
                                }
                            }
                        }
                        i += 30;
                    }
                    if ((mask_15 & 0x20) == 0x20)
                    {//ECU Aftertreatment
                        ecuSt = (char) taipZip[i + 2];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZA=D";
                        }
                        else if (ecuSt == 'u')
                        {
                            toRet += ";ZA=U";
                        }
                        else
                        {
                            toRet += ";ZA=";
                            for (int k = 0; k < 9; k++)
                            {
                                ecuSt = (char) taipZip[(k * 3) + i + 2];
                                if (ecuSt != 'U')
                                {
                                    value = (data[(k * 3) + i] << 8) | data[(k * 3) + i + 1];
                                    if ((data[(k * 3) + i] & 0x80) == 0x80)
                                    {//Its a negative value
                                        value = value - 65536;
                                    }
                                    toRet += "" + ecuSt + value;
                                }
                                if (k + 1 < 9)
                                {
                                    toRet += ',';
                                }
                            }
                        }
                        i += 36;
                    }

                    if ((mask_15 & 0x40) == 0x40)
                    {//ECU Exhaust
                        ecuSt = (char) taipZip[i + 2];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZX=D";
                        }
                        else if (ecuSt == 'u')
                        {
                            toRet += ";ZX=U";
                        }
                        else
                        {
                            toRet += ";ZX=";
                            for (int k = 0; k < 7; k++)
                            {
                                ecuSt = (char) taipZip[(k * 3) + i + 2];
                                if (ecuSt != 'U')
                                {
                                    value = (data[(k * 3) + i] << 8) | data[(k * 3) + i + 1];
                                    if ((data[(k * 3) + i] & 0x80) == 0x80)
                                    {//Its a negative value
                                        value = value - 65536;
                                    }
                                    toRet += "" + ecuSt + value;
                                }
                                if (k + 1 < 7)
                                {
                                    toRet += ',';
                                }
                            }
                        }
                        i += 27;
                    }

                    if ((mask_15 & 0x80) == 0x80)
                    {//ECU Engine
                        ecuSt = (char) taipZip[i + 2];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZE=D";
                        }
                        else if (ecuSt == 'u')
                        {
                            toRet += ";ZE=U";
                        }
                        else
                        {
                            toRet += ";ZE=";
                            for (int k = 0; k < 9; k++)
                            {
                                ecuSt = (char) taipZip[(k * 3) + i + 2];
                                if (ecuSt != 'U')
                                {
                                    value = (data[(k * 3) + i] << 8) | data[(k * 3) + i + 1];
                                    if ((data[(k * 3) + i] & 0x80) == 0x80)
                                    {//Its a negative value                                        
                                        if (k != 6)//DID_ENGINE_REF_TORQ = k = 6 (0 - 65535)
                                        {
                                            value = value - 65536;
                                        }
                                    }
                                    toRet += "" + ecuSt + value;
                                }
                                if (k + 1 < 9)
                                {
                                    toRet += ',';
                                }
                            }
                        }
                        i += 36;
                    }
                }
                if ((newGroup & 0x80) == 0x80)
                {
                    short mask_16 = data[i++];
                    System.out.println("DecodingGroup16 " + Integer.toBinaryString(mask_16 & 0xFF) + " i=" + i);
                    if ((mask_16 & 0x01) == 0x01)
                    {//ECU Turbocharger
                        ecuSt = (char) taipZip[i + 1];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZH=D";
                        }
                        else if (ecuSt == 'u')
                        {
                            toRet += ";ZH=U";
                        }
                        else
                        {
                            toRet += ";ZH=";
                            int base;
                            for (int k = 0; k < 2; k++)
                            {
                                base = (k * 2);
                                ecuSt = (char) taipZip[base + i + 1];
                                if (ecuSt != 'U')
                                {
                                    value = (data[base + i]) & 0x7F;
                                    toRet += "" + ecuSt + value;
                                }
                                toRet += ",";
                            }
                            for (int k = 2; k < 4; k++)
                            {
                                base = (k * 2) + (k - 2);
                                ecuSt = (char) taipZip[base + i + 2];
                                if (ecuSt != 'U')
                                {
                                    value = (data[base + i] << 8) | data[base + i + 1];
                                    if ((data[base + i] & 0x80) == 0x80)
                                    {//Its a negative value
                                        value = value - 65536;
                                    }
                                    toRet += "" + ecuSt + value;
                                }
                                toRet += ",";
                            }
                            base = 10;
                            ecuSt = (char) taipZip[base + i + 3];
                            if (ecuSt != 'U')
                            {
                                value = (((data[base + i] << 8) | data[base + i + 1]) << 8) | data[base + i + 2];
                                if ((data[base + i] & 0x80) == 0x80)
                                {//Its a negative value
                                    value = value - 16777216;
                                }
                                toRet += "" + ecuSt + value;
                            }
                        }
                        i += 20;
                    }
                    if ((mask_16 & 0x02) == 0x02)
                    {// ECU Run time
                        ecuSt = (char) taipZip[i + 4];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZR=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZR=U";
                        }
                        else
                        {
                            value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                            toRet += ";ZR=" + ecuSt + value;
                        }
                        i += 5;
                    }
                    if ((mask_16 & 0x04) == 0x04)
                    {// ECU trip fuel
                        ecuSt = (char) taipZip[i + 4];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZG=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZG=U";
                        }
                        else
                        {
                            value = (((((data[i] << 8) | data[i + 1]) << 8) | data[i + 2]) << 8) | data[i + 3];
                            toRet += ";ZG=" + ecuSt + value;
                        }
                        i += 5;
                    }
                    if ((mask_16 & 0x08) == 0x08)
                    {//Continental Tire: Tires under warning state
                        ecuSt = (char) taipZip[i + 120];
                        if (ecuSt == 'd')
                        {
                            toRet += ";ZY=D";
                        }
                        else if (ecuSt == 'U')
                        {
                            toRet += ";ZY=U";
                        }
                        else
                        {

                            int location;
                            String tireInfo = "";
                            for (int k = 0; k < 121; k += 5)
                            {
                                location = (data[i + k]);
                                if (location == 0x0F)
                                {
                                    break;
                                }

                                //There is information in this position
                                value = (int) Utils.byteArrayToLong(taipZip, i + 1 + k, 4);
                                if (value != 0)
                                {
                                    tireInfo += "," + Utils.integerF(location, 3);
                                    //Mute and defective
                                    tireInfo += value & 0x03;
                                    //Tire leak
                                    tireInfo += (value >> 2) & 0x03;
                                    //Sensor state
                                    tireInfo += (value >> 4) & 0x03;
                                    //Temp Warning 
                                    tireInfo += (value >> 6) & 0x03;
                                    //Tire leakage rate                                    
                                    //tireInfo += Format.integerF((int) ((value >> 8) & 0xFFFF), 5);
                                    //System ID
                                    tireInfo += (value >> 24) & 0x03;
                                    //Pressure
                                    tireInfo += (value >> 26) & 0x07;
                                    //Valid
                                    tireInfo += (value >> 29) & 0x01;
                                    //Reserved
                                    //tireInfo += (value >> 30) & 0x03;
                                }
                            }
                            if (tireInfo.length() > 1)
                            {
                                tireInfo = tireInfo.substring(1);
                            }
                            else if (ecuSt != 'D')
                            {
                                ecuSt = 'U';
                            }

                            toRet += ";ZY=" + ecuSt + tireInfo;
                        }
                        i += 121;
                    }
                    if ((mask_16 & 0x10) == 0x10)
                    {// Flow meter
                        if (data[i] == 0xFF)
                        {
                            toRet += ";FM=U";
                        }
                        else
                        {
                            String values = "";
                            value = 0;
                            for (int j = 0, pos = 0; j < 8; j++)
                            {
                                switch (j)
                                {
                                    case 0:
                                    case 1:
                                        value = data[i + pos];
                                        pos++;
                                        break;
                                    case 2:
                                    case 3:
                                    case 4:
                                    case 5:
                                        value = (data[i + pos + 1] << 8) | (data[i + pos]);
                                        pos += 2;
                                        break;
                                    case 6:
                                    case 7:
                                        value = (data[i + pos + 3] << 24) | (data[i + pos + 2] << 16) | (data[i + pos + 1] << 8) | (data[i + pos]);
                                        pos += 4;
                                        break;
                                }
                                values += "," + value;
                            }
                            toRet += ";FM=" + values.substring(1);
                        }
                        i += 18;
                    }
                }
            }
        }
        catch (Exception e)
        {
            System.out.println("wrongDataExc " + e.toString());
            //#if _PRINT_STACK_TRACE == 1
//#             e.printStackTrace();
            //#endif
            return "";
        }
        System.out.println("dataSize[" + data.length + "] i[" + i + "]");
        if (data.length != i)
        {//Wrong data
            System.out.println("wrongData");
            return "";
        }
        return toRet;
    }

    private static byte getSADecoded(int value)
    {
        byte sa = 99;
        switch (value)
        {
            case 1:
                sa = 2;
                break;
            case 2:
                sa = 11;
                break;
            case 3:
                sa = 12;
                break;
            case 4:
                sa = 22;
                break;
            case 5:
                sa = 32;
                break;
            case 6:
                sa = 90;
                break;
            default:
                break;
        }
        return sa;
    }
}
