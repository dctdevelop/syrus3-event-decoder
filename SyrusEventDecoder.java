/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 *
 * @author syrus
 */
public class SyrusEventDecoder
{

    public static final int MAX_MESS_SIZE = 1200;
    public static final int HEAD_SIZE = 4;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        //System.out.println("Event decoder started!");
        if (null == args || args.length < 1)
        {
            System.out.println("Please specify the file name");
            return;
        }
        try
        {
            File event_f = new File(args[0]);
            FileInputStream file_is = new FileInputStream(event_f);
            if (event_f.length() > 0)
            {
                int i = 1;
                while (true)
                {
                    byte[] head = new byte[HEAD_SIZE];
                    if (file_is.read(head, 0, HEAD_SIZE) != HEAD_SIZE)
                    {
                        System.out.println(" " + i + " normal end");
                        break;
                    }

                    // int size = (int)sizeB[0];		    
                    int size = 0x000000FF & (int) head[0];
                    size *= 256;
                    size += 0x000000FF & (int) head[1];

                    if (size <= 0 || size > MAX_MESS_SIZE)
                    {
                        System.out.println(" " + i + " by size " + size);
                        break;
                    }

                    if (head[2] != 1 || head[3] != 0)
                    {//No valid head
                        System.out.println(" " + i + " by head " + size);
                        break;
                    }

                    byte[] mess = new byte[size - 4];

                    if (file_is.read(mess, 0, mess.length) != mess.length)
                    {
                        System.out.println(" " + i + " by mess len " + mess.length);
                        break;
                    }
                    String ev = Decoder.decodeEventWithExtendedInfo(mess);
                    System.out.println(ev);
                    i++;
                    if (file_is.available() > 0)
                    {
                        //System.out.println(" " + i + " available " + file_is.available());
                        continue;
                    }
                    break;
                }
                file_is.close();
            }
        }
        catch (IOException ex)
        {
            System.out.println("IOException " + ex.toString());
            ex.printStackTrace();
        }
    }
}
