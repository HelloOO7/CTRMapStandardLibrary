
package ctrmap.stdlib.text;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility methods for formatting text.
 */
public class FormattingUtils {
	
	public static String getFormattedHexString32(int number) {
		String hexstring = Integer.toHexString(Integer.reverseBytes(number));
		return ("00000000" + hexstring).substring(hexstring.length()).toUpperCase().replaceAll("..", "$0 ");
	}

	public static String getFormattedHexString32LE(int number) {
		String hexstring = Integer.toHexString(number);
		return ("00000000" + hexstring).substring(hexstring.length()).toUpperCase().replaceAll("..", "$0 ");
	}

	public static String getFormattedHexStringShort(int number) {
		String hexstring = Integer.toHexString(number);
		return ("0000" + hexstring).substring(hexstring.length()).toUpperCase().replaceAll("..", "$0 ");
	}

	/**
	 * Formats a date with the YYYY-MM-DD HH:MM format.
	 * @return A String with the formatted current RTC date.
	 */
	public static String getCommonFormattedDate(){
		return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
	}
	
	/**
	 * Creates a String from a string literal as read from a text file.
	 * The method returns null if the String is literal 'null', but returns "null" if the literal 'null' is enclosed in quotation marks.
	 * Quotation marks that have been escaped with '\' in the literal will have the escape character removed.
	 * @param str The string literal.
	 * @return A sanitized Java String.
	 */
	public static String makeStringFromLiteral(String str){
		if (str.equals("null")){
			return null;
		}
		if (str.startsWith("\"") && str.endsWith("\"")){
			str = str.substring(1, str.length() - 1).replace("\\\"", "\"");
		}
		return str;
	}
	
	/**
	 * Adds leading zeros to an integer.
	 * @param zeroCount Maximum number of leading zeros.
	 * @param value The value to represent.
	 * @return A String representation of the value with up to 'zeroCount' leading zeros.
	 */
	public static String getIntWithLeadingZeros(int zeroCount, int value) {
		return getStrWithLeadingZeros(zeroCount, String.valueOf(value));
	}
	
	/**
	 * Adds leading zeros to a String.
	 * @param zeroCount Maximum number of leading zeros.
	 * @param value The String to prepend the zeros to.
	 * @return 'value' prefixed with up to 'zeroCount' leading zeros.
	 */
	public static String getStrWithLeadingZeros(int zeroCount, String value) {
		StringBuilder zeroSB = new StringBuilder();
		for (int i = 0; i < zeroCount; i++) {
			zeroSB.append("0");
		}
		String v = value.substring(0, Math.min(value.length(), zeroCount));
		return zeroSB.substring(v.length()) + v;
	}

	/**
	 * Converts a file size integer to an user-friendly unit representation.
	 * @param size Size in bytes.
	 * @return String representation of 'size', in bytes, kB or MB.
	 */
	public static String getFriendlySize(int size) {
		if (size < 1000) {
			return size + " bytes";
		}
		DecimalFormat f = new DecimalFormat();
		f.setMaximumFractionDigits(2);
		if (size < 1000000) {
			return f.format(size / (double) 1000) + "kB";
		}
		return f.format(size / (double) 1000000) + "MB";
	}
	
	/**
	 * Substitutes all non-alphanumeric characters in a String with underscores.
	 * @param str A String.
	 * @return The String with each block of non-alphanumeric characters replaced with an underscore.
	 */
	public static String getStrWithoutNonAlphanumeric(String str){
		StringBuilder sb = new StringBuilder();
		boolean appendNextUnderscore = false;
		for (int i = 0; i < str.length(); i++){
			char c = str.charAt(i);
			if (Character.isLetterOrDigit(c)){
				if (appendNextUnderscore){
					sb.append('_');
					appendNextUnderscore = false;
				}
				sb.append(c);
			}
			else {
				appendNextUnderscore = true;
			}
		}
		return sb.toString();
	}
	
	/**
	 * Converts a 'camelCase' String to 'PascalCase'.
	 * @param str A camelCase String.
	 * @return The input String in PascalCase.
	 */
	public static String camelToPascal(String str){
		if (str == null || str.isEmpty()){
			return str;
		}
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}
	
	/**
	 * Makes a String follow Java's enum naming conventions.
	 * @param str A String.
	 * @return THE_INPUT_STRING_WITHOUT_SPACES_AND_IN_UPPERCASE.
	 */
	public static String getEnumlyString(String str){
		return getStrWithoutNonAlphanumeric(str.trim()).toUpperCase();
	}

	/**
	 * Converts an enum name that follows Java's enum naming conventions to a more user-friendly representation.
	 * The output String will be likely similar to C#'s enum naming convention.
	 * All underscores will be eliminated and their effect substituted by making the character after them uppercase.
	 * The first character will always be uppercase.
	 * @param e An enum value.
	 * @return Hopefully a more user-friendly name of the enum.
	 */
	public static String getFriendlyEnum(Enum e) {
		String lc = e.name().toLowerCase();
		StringBuilder sb = new StringBuilder();
		boolean isNextUpperCase = true;
		for (int i = 0; i < lc.length(); i++) {
			char c = lc.charAt(i);
			if (c == '_') {
				isNextUpperCase = true;
			} else {
				sb.append(isNextUpperCase ? Character.toUpperCase(c) : c);
				isNextUpperCase = false;
			}
		}
		return sb.toString();
	}

}
