package absent_minded.absent_minded.controllers;

import absent_minded.absent_minded.models.Project;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DemoController {
    @GetMapping("/demo")
    public String demo() { return "hello absent minded"; }
}
