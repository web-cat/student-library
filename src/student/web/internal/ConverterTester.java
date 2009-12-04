package student.web.internal;

import student.web.WebUtilities;

public class ConverterTester
{
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
        String[] names = {"Stephen Edwards", "Fred Flintstone"};

        for (String name : names)
        {
            String encoded = sanitizeId(name);
            String decoded = unsanitizeId(encoded);
            System.out.println(
                '"' + name
                + "\" => \"" + encoded
                + "\" => \"" + decoded
                + '"');
        }
    }
}
