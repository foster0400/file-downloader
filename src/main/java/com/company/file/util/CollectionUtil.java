package com.company.file.util;

import java.util.Collection;

public class CollectionUtil {
  public static boolean isEmpty(Collection<?> coll) {
    return coll == null || coll.isEmpty();
  }
}
