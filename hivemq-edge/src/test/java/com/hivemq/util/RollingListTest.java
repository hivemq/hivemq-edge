package com.hivemq.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Simon L Johnson
 */
public class RollingListTest {


    @Test
    public void test_rolling_list_limits() throws Exception {

        //default capacity is 25
        RollingList<Integer> list = new RollingList<>();
        fill(list, 25);
        Assert.assertEquals("List should contain 25 elements", 25, list.size());
        //add 26th element
        list.add(25);
        Assert.assertEquals("List should contain 25 elements", 25, list.size());
        Assert.assertEquals("Index zero should now be the latest", (Integer) 25, list.get(0));
        Assert.assertEquals("Index zero should now be the latest", (Integer) 24, list.get(list.size() - 1));
    }

    @Test
    public void test_rolling_list_variable_limits() throws Exception {

        RollingList<Integer> list = new RollingList<>(100);
        fill(list, 100);
        Assert.assertEquals("List should contain 100 elements", 100, list.size());
        //add 101th element
        list.add(100);
        Assert.assertEquals("List should contain 100 elements", 100, list.size());
        Assert.assertEquals("Index zero should now be the latest", (Integer) 100, list.get(0));
        Assert.assertEquals("Index zero should now be the latest", (Integer) 99, list.get(list.size() - 1));

        fill(list, 1000);
        Assert.assertEquals("List should contain 100 elements", 100, list.size());
    }


    private static void fill(RollingList<Integer> list, int count){
        for (int i = 0; i < count; i++)
            list.add(i);
    }
}
