/**
 *
 */
package com.dianwoda.usercenter.vera.piper;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;

//@RunWith(SpringRunner.class)
//@SpringBootTest
//@ActiveProfiles(value = "hz-unit1")
public class UnitTestBase {

  @Before
  public void before() {


  }

  static boolean delFile(File file) {
    if (!file.exists()) {
      return false;
    }

    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (File f : files) {
        delFile(f);
      }
    }
    return file.delete();
  }

}
