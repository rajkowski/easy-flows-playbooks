package org.jeasy.flows.work;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ExpressionTest {

  @Test
  public void expressionTest() {
    Map<String, Object> order = new HashMap<>();
    order.put("live", true);
    order.put("uniqueId", "0000-0000-0000");

    JexlContext mapContext = new MapContext();
    mapContext.set("order", order);

    String subject = (String) Expression.evaluate(mapContext, "{{ order.live ? \"TEST \" : \"\" }}New order # {{ order.uniqueId }}");
    Assert.assertEquals("TEST New order # 0000-0000-0000", subject);
  }
}
