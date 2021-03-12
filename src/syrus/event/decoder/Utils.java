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
public class Utils
{

    private final static int J1708 = 2;

    /**
     * Format an integer
     *
     * @param value
     * @param chars
     * @return An String with the number formated
     */
    public static String integerF(int value, int chars)
    {
        String data;
        int lenght;

        data = Integer.toString(value);
        lenght = data.length();
        for (int i = lenght; i < chars; i++)
        {
            data = "0" + data;
        }
        return data;
    }

    /**
     * Converts up to 8 consecutive positions of an array to long.
     *
     * @param array
     * @param i     : index of the first position in the array
     * @param bytes : Number of bytes to convert
     *
     * @return long
     */
    public static long byteArrayToLong(byte[] array, int i, int bytes)
    {
        if (array.length < (i + bytes))
        {
            return 0;
        }
        long value_0 = 0;
        long value_1 = 0;
        long value_2 = 0;
        long value_3 = 0;
        long value_4 = 0;
        long value_5 = 0;
        long value_6 = 0;
        long value_7 = 0;

        if (bytes > 0)
        {
            value_0 = ((long) array[i]) & 0xFFL;
        }
        if (bytes > 1)
        {
            value_1 = (((long) array[i + 1]) << 8) & 0xFF00L;
        }
        if (bytes > 2)
        {
            value_2 = (((long) array[i + 2]) << 16) & 0xFF0000L;
        }
        if (bytes > 3)
        {
            value_3 = (((long) array[i + 3]) << 24) & 0xFF000000L;
        }
        if (bytes > 4)
        {
            value_4 = (((long) array[i + 4]) << 32) & 0xFF00000000L;
        }
        if (bytes > 5)
        {
            value_5 = (((long) array[i + 5]) << 40) & 0xFF0000000000L;
        }
        if (bytes > 6)
        {
            value_6 = (((long) array[i + 6]) << 48) & 0xFF000000000000L;
        }
        if (bytes > 7)
        {
            value_7 = (((long) array[i + 7]) << 56) & 0xFF00000000000000L;
        }

        return value_0 + value_1 + value_2 + value_3 + value_4 + value_5 + value_6 + value_7;
    }

    /**
     * This method returns the hex representation of a byte array
     *
     * @param a
     * @return The hex string representation, i.e, 0A0F0031 = byte{10,15,0,49}
     */
    public static String toHexStringUpperCase(byte[] a)
    {

        if (a == null)
        {
            return "(null)";
        }

        String ret = "";

        for (int i = 0; i < a.length; i++)
        {
            ret += Integer.toString((a[i] & 0xff) + 0x100, 16).substring(1);
        }
        return ret.toUpperCase();
    }

    /**
     * Converts up to four consecutive positions of an array to a int.
     *
     * @param array
     * @param i     : index of the first position in the array
     * @param bytes : Number of bytes to convert
     *
     * @return int
     */
    public static int byteArrayToInt(byte[] array, int i, int bytes)
    {
        if (array.length < (i + bytes))
        {
            return 0;
        }
        int value_0 = 0;
        int value_1 = 0;
        int value_2 = 0;
        int value_3 = 0;

        if (bytes > 0)
        {
            value_0 = ((int) array[i]) & 0xFF;
        }
        if (bytes > 1)
        {
            value_1 = (((int) array[i + 1]) << 8) & 0xFF00;
        }
        if (bytes > 2)
        {
            value_2 = (((int) array[i + 2]) << 16) & 0xFF0000;
        }
        if (bytes > 3)
        {
            value_3 = (((int) array[i + 3]) << 24) & 0xFF000000;
        }

        return value_0 + value_1 + value_2 + value_3;
    }

    /**
     *
     * @param protocol
     * @param value
     *
     * @return The error code decoded: <br>
     * For 1708: SID or PID,FMI,Occurrence Counter,Current status of fault,Type,Identifier for standard code,Occurrence counter value<br>
     * For 1939: SPN,FMI,Conversion method,Frequency counter
     */
    public static String ecuDecodeErrorCode(int protocol, long value)
    {
        if (value == 0x00 || value == 0x00FFFFFFFF)
        {
            return "0,0,0,0";
        }

        int aux;
        String retMess;
        if (protocol == J1708)
        {
            //SID or PID
            aux = (int) ((value >> 16) & 0x00FF);
            retMess = aux + ",";
            //FMI
            aux = (int) ((value >> 8) & 0x000F);
            retMess += aux + ",";
            //Occurrence Counter: 1 Included, 0 Not included 
            aux = (int) ((value >> 15) & 0x0001);
            retMess += aux + ",";
            //Current status of fault: 1 Inactive, 0 Active 
            aux = (int) ((value >> 14) & 0x0001);
            retMess += aux + ",";
            //Type of diagnostic code: 1 Standard, 0 Expansion  
            aux = (int) ((value >> 13) & 0x0001);
            retMess += aux + ",";
            //Identifier for standard code: 1 SID, 0 PID 
            aux = (int) ((value >> 12) & 0x0001);
            retMess += aux + ",";
            //Occurrence counter 
            aux = (int) value & 0x00FF;
            retMess += aux;
        }
        else
        {//1939
            int convMethod;
            //Conversion method 
            convMethod = (int) ((value >> 7) & 0x0001);
            if (convMethod == 0)
            {
                //SPN
                aux = (int) (((value >> 24) & 0x00FF) | ((value >> 8) & 0x00FF00) | ((value << 3) & 0x070000));
                retMess = aux + ",";
            }
            else
            {
                //SPN
                aux = (int) ((value >> 13) & 0x07FFFF);
                retMess = aux + ",";
            }

            //FMI
            aux = (int) ((value >> 8) & 0x001F);
            retMess += aux + ",";
            //Add Conversion method						
            retMess += convMethod + ",";
            //Frequency counter 
            aux = (int) (value & 0x007F);
            retMess += aux;
        }
        return retMess;
    }

    /**
     *
     * @param rawData
     * @return ABCDEEEFGGGHIJKLMN, where:<br>
     * A: Sound type<br>
     * B: Time indicator<br>
     * C: Zero Speed<br>
     * D: Headway valid<br>
     * EEE: Headway measurement<br>
     * F: Error flag<br>
     * GGG: Error code<br>
     * H: Lane departure off<br>
     * I: Maintenance flag<br>
     * J: Fail safe flag<br>
     * K: Traffic signal recognition enabled flag<br>
     * L: Traffic signal recognition warning level<br>
     * M: Headway Warning level<br>
     * N: HW repeatable<br>
     */
    public static String mobileyeDecode700(byte[] rawData)
    {
        if (rawData == null || rawData.length != 8)
        {
            return "";
        }
        //Parameters 
        String state = "";
        int temp = ((int) rawData[0]) & 0xFF;
        //Sound type
        state += ((temp) & 0x07);
        //Time indicator
        state += ((temp >> 3) & 0x03);
        temp = ((int) rawData[1]) & 0xFF;
        //Zero Speed		
        state += testBitInt(temp, 5);
        temp = ((int) rawData[2]) & 0xFF;
        //Headway Valid
        state += testBitInt(temp, 0);
        //Headway measurement
        state += integerF(((temp >> 1) & 0x7F), 3);
        temp = ((int) rawData[3]) & 0xFF;
        //Error Flag
        state += testBitInt(temp, 0);
        //Error Code		
        state += integerF(((temp >> 1) & 0x7F), 3);
        temp = ((int) rawData[4]) & 0xFF;
        //Lane departure Off
        state += testBitInt(temp, 0);
        //Maintenance flag
        state += testBitInt(temp, 6);
        //Fail safe flag
        state += testBitInt(temp, 7);
        temp = ((int) rawData[5]) & 0xFF;
        //Traffic signal recognition enabled flag
        state += testBitInt(temp, 7);
        temp = ((int) rawData[6]) & 0xFF;
        //Traffic signal recognition warning level
        state += (temp & 0x07);
        temp = ((int) rawData[7]) & 0xFF;
        //Headway warning level
        state += (temp & 0x03);
        //HW repeatable 
        state += testBitInt(temp, 2);
        return state;
    }

    /**
     *
     * @param rawData
     * @return OPQRSTUVWXYYY, where:<br>
     * O: Brakes<br>
     * P: Left signal<br>
     * Q: Right signal<br>
     * R: Wipers<br>
     * S: Low Beam<br>
     * T: High Beam<br>
     * U: Wipers Available<br>
     * V: Low Beam Available<br>
     * W: High Beam Available<br>
     * X: Speed Available<br>
     * YYY: Speed<br>
     */
    public static String mobileyeDecode760(byte[] rawData)
    {
        if (rawData == null || rawData.length != 3)
        {
            return "";
        }
        String state = "";
        int temp = ((int) rawData[0]) & 0xFF;
        //Brakes
        state += testBitInt(temp, 0);
        //Left signal
        state += testBitInt(temp, 1);
        //Right signal
        state += testBitInt(temp, 2);
        //Wipers
        state += testBitInt(temp, 3);
        //Low beam
        state += testBitInt(temp, 4);
        //High beam
        state += testBitInt(temp, 5);
        temp = ((int) rawData[1]) & 0xFF;
        //Wipers available
        state += testBitInt(temp, 3);
        //Low beam available
        state += testBitInt(temp, 4);
        //High beam available
        state += testBitInt(temp, 5);
        //Speed Available
        state += testBitInt(temp, 7);
        temp = ((int) rawData[2]) & 0xFF;
        //Speed
        state += integerF(temp, 3);
        return state;
    }

    /**
     *
     * @param pkg the byte[] to be decoded or null for returning the last information received
     * @return a String as follows:
     */
    public static String movonGetDataDecoded(byte[] pkg)
    {
        String toRet = "";
        if (pkg == null)
        {
            return null;
        }
        int temp;
        //Speed (0 - 255 / 0x00 – 0xFF)
        temp = pkg[0] & 0xFF;
        toRet += integerF(temp, 3);
        //Left turn signal: 0x00 – None, 0x01 – Left turn signal On	
        toRet += pkg[1] & 0x01;
        //Right turn signal: 0x00 – None, 0x01 – Right turn signal On
        toRet += pkg[2] & 0x01;
        //Brake signal: 0x00 – None, 0x01 – Brake On
        toRet += pkg[3] & 0x01;
        //RPM: 0 - 65535, RPM[0] MSB / RPM[1] LSB
        temp = ((pkg[4] & 0xFF) << 8) | (pkg[5] & 0xFF);
        toRet += integerF(temp, 5);
        //Lane departure warning left: 0x00 – None, 0x01 – Recognized, 0x02 – Left LDW event, 0x03 – Function disabled 
        toRet += pkg[6] & 0x03;
        //Lane departure warning right: 0x00 – None, 0x01 – Recognized, 0x02 – Right LDW event, 0x03 – Function disabled 
        toRet += pkg[7] & 0x03;
        //Left distance: Length of left lane (cm), Left[0] MSB / Left[1] LSB 
        temp = ((pkg[8] & 0xFF) << 8) | (pkg[9] & 0xFF);
        toRet += integerF(temp, 5);
        //Right distance: Length of right lane (cm), Right[0] MSB / Right[1] LSB 
        temp = ((pkg[10] & 0xFF) << 8) | (pkg[11] & 0xFF);
        toRet += integerF(temp, 5);
        //TTC time, 0 - 25.5 sec, Ex) 125 => 12.5sec
        temp = pkg[12] & 0xFF;
        toRet += integerF(temp, 3);
        //SDA - Safety distance alarm : 0x00 – None, 0x01 – Recognized (ahead vehicle), 0x02 – SDA event, 0x03 – Function disabled
        toRet += pkg[13] & 0x03;
        //Front vehicle start alarm: 0x00 – None, 0x02 – FVSA event, 0x03 – Function disabled
        toRet += pkg[14] & 0x03;
        //Forward proximity warning: 0x00 – None, 0x02 – FPW event, 0x03 – Function disabled
        toRet += pkg[15] & 0x03;
        //Forward collision warning: 0x00 – None, 0x02 – FCW event, 0x03 – Function disabled
        toRet += pkg[16] & 0x03;
        //Pedestrian collision warning: 0x00 – None, 0x01 – Recognized, 0x02 – PCW event, 0x03 – Function disabled
        toRet += pkg[17] & 0x03;
        //Record: 0x00 – None, 0x01 – Recording (Mic off), 0x02 – Recording (Mic On)
        toRet += pkg[18] & 0x03;
        //Error code: 0x00 – None, 0x01 – Low visibility, 0x02 – Camera blocked 
        toRet += pkg[19] & 0x03;
        return toRet;
    }

    /**
     * Tests the "nBit" bit of "number"
     *
     * @param number : Integer
     * @param nBit   : n bit to be tested
     *
     * @return int : Value of the "nBit" bit of "number" (0 or 1) -1 : On error
     */
    public static int testBitInt(int number, int nBit)
    {

        if ((nBit < 0) || (nBit > 31))
        {
            return -1;
        }

        return ((number & (1 << nBit))) >> nBit;
    }
}
