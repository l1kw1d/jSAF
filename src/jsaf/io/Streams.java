// Copyright (c) 2011 jOVAL.org.  All rights reserved.
// This software is licensed under the LGPL 3.0 license available at http://www.gnu.org/licenses/lgpl.txt

package jsaf.io;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Vector;

import jsaf.Message;
import jsaf.util.Strings;

/**
 * Some stream utilities.
 *
 * @author David A. Solin
 * @version %I% %G%
 * @since 1.2
 */
public class Streams {
    /**
     * Useful in debugging...
     *
     * @since 1.2
     */
    public static final void hexDump(byte[] buff, PrintStream out) {
	int numRows = buff.length / 16;
	if (buff.length % 16 > 0) numRows++; // partial row

	int ptr = 0;
	for (int i=0; i < numRows; i++) {
	    for (int j=0; j < 16; j++) {
		if (ptr < buff.length) {
		    if (j > 0) System.out.print(" ");
		    String iHex = Integer.toHexString((int)buff[ptr++]);
		    if (iHex.length() == 0) {
			out.print("0");
		    }
		    out.print(iHex);
		} else {
		    break;
		}
	    }
	    out.println("");
	}
    }

    /**
     * Read from the stream until the buffer is full.
     *
     * @since 1.2
     */
    public static final void readFully(InputStream in, byte[] buff) throws IOException {
	int offset = 0;
	do {
	    int bytesRead = in.read(buff, offset, buff.length-offset);
	    if (bytesRead == 0) {
	        throw new EOFException(Message.getMessage(Message.ERROR_EOS));
	    } else {
		offset += bytesRead;
	    }
	} while (offset < buff.length);
    }

    /**
     * Copy from in to out asynchronously (i.e., in a new Thread). Closes the InputStream when done, but not
     * the OutputStream.
     *
     * @return the new Thread
     *
     * @since 1.2
     */
    public static Thread copyAsync(InputStream in, OutputStream out) {
	Thread thread = new Thread(new Copier(in, out));
	thread.start();
	return thread;
    }

    /**
     * Copy completely from in to out.  Closes the InputStream when done, but not the OutputStream.
     *
     * @since 1.2
     */
    public static void copy(InputStream in, OutputStream out) {
	copy(in, out, false);
    }

    /**
     * Copy completely from in to out.  Closes the InputStream when done.  Closes the OutputStream according to closeOut.
     *
     * @since 1.2
     */
    public static void copy(InputStream in, OutputStream out, boolean closeOut) {
	try {
	    new Copier(in, out).run();
	} finally {
	    if (closeOut && out != null) {
		try {
		    out.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    /**
     * Get an OutputStream to nowhere.
     *
     * @since 1.3
     */
    public static OutputStream devNull() {
	return DEVNULL;
    }

    /**
     * Read the BOM (Byte-Order marker) from a stream.
     *
     * @since 1.2
     */
    public static Charset detectEncoding(InputStream in) throws IOException {
	switch(in.read()) {
	  case 0xEF:
	    if (in.read() == 0xBB && in.read() == 0xBF) {
		return Strings.UTF8;
	    }
	    break;
	  case 0xFE:
	    if (in.read() == 0xFF) {
		return Strings.UTF16;
	    }
	    break;
	  case 0xFF:
	    if (in.read() == 0xFE) {
		return Strings.UTF16LE;
	    }
	    break;
	}
	throw new java.nio.charset.CharacterCodingException();
    }

    // Private

    private static class Copier implements Runnable {
	InputStream in;
	OutputStream out;

	Copier(InputStream in, OutputStream out) {
	    this.in = in;
	    this.out = out;
	}

	// Implement Runnable

	public void run() {
	    try {
		byte[] buff = new byte[1024];
		int len = 0;
		while ((len = in.read(buff)) > 0) {
		    out.write(buff, 0, len);
		}
	    } catch (IOException e) {
	    } finally {
		try {
		    in.close();
		} catch (IOException e) {
		}
	    }
	}
    }

    private static final OutputStream DEVNULL = new DevNull();

    private static class DevNull extends OutputStream {
	private DevNull() {
	}

	public void write(int b) {
	}

	public void write(byte[] b) {
	}

	public void write(byte[] b, int offset, int len) {
	}
    }
}
