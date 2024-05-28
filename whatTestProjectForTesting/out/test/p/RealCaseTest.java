import org.example.Main;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class MainTest {


    @Test
    public void demoTestMethod() {
        Main main = new Main();
        assertEquals(7, main.add(6,3));
    }
    @Test
    public void demoMulMethod() {
        Main main = new Main();

        assertEquals(15, main.mul(4,5));
    }

    @Test
    public void demoDivMethod() {
        Main main = new Main();

        assertEquals(2, main.div(4,2));
    }

    @Test
    public void demoDiv2Method() {
        Main main = new Main();

        assertEquals(2, main.div(6,3));
    }
}
