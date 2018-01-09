package de.frauas.scs.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ThrowableConverter {

	public static String convertStackTrace(Exception exception)
	{
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		exception.printStackTrace(printWriter);
		return stringWriter.toString();
	}
}
