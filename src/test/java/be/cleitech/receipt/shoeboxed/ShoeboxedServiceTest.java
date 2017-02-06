package be.cleitech.receipt.shoeboxed;

import be.cleitech.receipt.shoeboxed.domain.ShoeboxedTokenInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * Created by pierrick on 06.02.17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest()
public class ShoeboxedServiceTest {

    @Autowired
    ShoeboxedService shoeboxedService;
    @Test
    public void initAccessToken() throws Exception {
        ShoeboxedTokenInfo accessTokenInfo = shoeboxedService.getAccessTokenInfo();
        System.out.println(accessTokenInfo);

    }

}