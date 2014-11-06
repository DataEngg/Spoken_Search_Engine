package org.galagosearch.core.tools;

import java.io.InputStream;

final public class ResouceLoader {
  public static InputStream load(String path)
  {
	  InputStream is=ResouceLoader.class.getResourceAsStream(path);
	  if(is==null)
	  {
		  is=ResouceLoader.class.getResourceAsStream("/"+path);
	  }
	  return is;
  }
}
