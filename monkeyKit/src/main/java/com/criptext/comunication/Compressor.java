package com.criptext.comunication;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Compressor {

	public Compressor()
	{}

	public byte[] gzipCompress(byte[] bytesToCompress) throws IOException {

		ByteArrayOutputStream os = new ByteArrayOutputStream(bytesToCompress.length);
		GZIPOutputStream gos = new GZIPOutputStream(os);

		gos.write(bytesToCompress);
		gos.close();
		byte[] compressed = os.toByteArray();
		os.close();

		return compressed;
	}

	public byte[] gzipDeCompress(byte[] bytesToDeCompress) throws IOException {
		
		ByteArrayInputStream bytein = new ByteArrayInputStream(bytesToDeCompress);
		GZIPInputStream gzis = new GZIPInputStream(bytein);
		ByteArrayOutputStream byteout = new ByteArrayOutputStream();
		
		int res = 0;
		byte buf[] = new byte[1024];
		while (res >= 0) {
		    res = gzis.read(buf, 0, buf.length);
		    if (res > 0) {
		        byteout.write(buf, 0, res);
		    }
		}
		
		return byteout.toByteArray();		
	}

	public byte[] compress(byte[] bytesToCompress)
	{		
		Deflater deflater = new Deflater();
		deflater.setInput(bytesToCompress);
		deflater.finish();

		byte[] bytesCompressed = new byte[Short.MAX_VALUE];

		int numberOfBytesAfterCompression = deflater.deflate(bytesCompressed);

		byte[] returnValues = new byte[numberOfBytesAfterCompression];

		System.arraycopy
		(
				bytesCompressed,
				0,
				returnValues,
				0,
				numberOfBytesAfterCompression
				);

		return returnValues;
	}

	public byte[] compressString(String stringToCompress)
	{		
		byte[] returnValues = null;

		try
		{

			returnValues = this.compress
					(
							stringToCompress.getBytes("UTF-8")
							);
		}
		catch (UnsupportedEncodingException uee)
		{
			uee.printStackTrace();
		}

		return returnValues;
	}

	public byte[] compressBytes(byte[] bytesToCompress)
	{		
		byte[] returnValues = null;
		returnValues = this.compress(bytesToCompress);
		return returnValues;
	}

	public byte[] decompress(byte[] bytesToDecompress)
	{
		byte[] returnValues = null;

		Inflater inflater = new Inflater();

		int numberOfBytesToDecompress = bytesToDecompress.length;

		inflater.setInput
		(
				bytesToDecompress,
				0,
				numberOfBytesToDecompress
				);

		int bufferSizeInBytes = numberOfBytesToDecompress;

		int numberOfBytesDecompressedSoFar = 0;
		List<Byte> bytesDecompressedSoFar = new ArrayList<Byte>();

		try
		{
			while (inflater.needsInput() == false)
			{
				byte[] bytesDecompressedBuffer = new byte[bufferSizeInBytes];

				int numberOfBytesDecompressedThisTime = inflater.inflate
						(
								bytesDecompressedBuffer
								);

				numberOfBytesDecompressedSoFar += numberOfBytesDecompressedThisTime;

				for (int b = 0; b < numberOfBytesDecompressedThisTime; b++)
				{
					bytesDecompressedSoFar.add(bytesDecompressedBuffer[b]);
				}
			}

			returnValues = new byte[bytesDecompressedSoFar.size()];
			for (int b = 0; b < returnValues.length; b++) 
			{
				returnValues[b] = (byte)(bytesDecompressedSoFar.get(b));
			}

		}
		catch (DataFormatException dfe)
		{
			dfe.printStackTrace();
		}

		inflater.end();

		return returnValues;
	}

	public String decompressToString(byte[] bytesToDecompress)
	{	
		byte[] bytesDecompressed = this.decompress
				(
						bytesToDecompress
						);

		String returnValue = null;

		try
		{
			returnValue = new String
					(
							bytesDecompressed,
							0,
							bytesDecompressed.length,
							"UTF-8"
							);	
		}
		catch (UnsupportedEncodingException uee)
		{
			uee.printStackTrace();
		}

		return returnValue;
	}
}
