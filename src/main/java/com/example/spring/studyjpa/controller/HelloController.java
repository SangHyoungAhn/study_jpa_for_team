import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HelloController {
    
    @GetMapping("/page/Hello")
    public String viewHello(){
        return "hello";
    }
}
