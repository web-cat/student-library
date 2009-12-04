package student.web.internal;

import java.io.File;
import student.web.WebUtilities;

public class CorrectFileNames
{
    private static final String EXT = ".dataxml";

    private static String sanitizeId(String id)
    {
        String result = null;
        if (result == null)
        {
            result = WebUtilities.urlEncode(id) + "-";
            int length = id.length();
            int marker = 0;
            for (int i = 0; i < length; i++)
            {
                if (Character.isUpperCase(id.charAt(i)))
                {
                    marker += 1 << (i % 4);
                }
                if (i % 4 == 3)
                {
                    result += Integer.toHexString(marker);
                    marker = 0;
                }
            }
            if (length % 4 > 0)
            {
                result += Integer.toHexString(marker);
            }
//            idCache.put(id, result);
//            idReverseCache.put(result, id);
        }
        return result;
    }


    // ----------------------------------------------------------
    private static String unsanitizeId(String id)
    {
        String result = null;
        if (result == null)
        {
            String encodedBase = id;
            String caps = "";
            int pos = id.lastIndexOf('-');
            if (pos > 0)
            {
                encodedBase = id.substring(0, pos);
                caps = id.substring(pos + 1);
            }
            String unencoded = WebUtilities.urlDecode(encodedBase);

            result = "";
            pos = 0;
            int length = caps.length();
            for (int i = 0; i < length && pos < unencoded.length(); i++)
            {
                int digit = Integer.parseInt(caps.substring(i, i + 1), 16);
                for (int j = 0; j < 4 && pos < unencoded.length(); j++)
                {
                    if ((digit & (1 << j)) != 0)
                    {
                        result +=
                            Character.toUpperCase(unencoded.charAt(pos++));
                    }
                    else
                    {
                        result +=
                            Character.toLowerCase(unencoded.charAt(pos++));
                    }
                }
            }
            if (pos < unencoded.length())
            {
                result += unencoded.substring(pos);
            }
        }
        return result;
    }

    public static void main(String[] args)
    {
        if (args.length != 1)
        {
            System.out.println("usage: java " + CorrectFileNames.class
                + " <dir>");
            return;
        }

        File dir = new File(args[0]);
        for (File file : dir.listFiles())
        {
            String name = file.getName();
            if (name.endsWith(EXT))
            {
                // Strip the extension
                name = name.substring(0, name.length() - EXT.length());

                // Now strip off the bungled caps info
                int pos = name.lastIndexOf('-');
                if (pos > 0)
                {
                    name = name.substring(0, pos);
                }

                // Restore to the original
                name = WebUtilities.urlDecode(name);

                // Compute new name
                String newName = sanitizeId(name) + EXT;
                File newFile = new File(file.getParentFile(), newName);
                file.renameTo(newFile);
            }
        }
    }
}
