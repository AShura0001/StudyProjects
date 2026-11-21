package uoa.se325.assignment01.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all paths which don't match our other routes, OTHER than for files, to index.html
 * This is required for proper enablement of client-side routing functionality with React Router.
 */
@Controller
public class ForwardController {

    @RequestMapping(value = "/{path:[^\\.]*}")
    public String forward() {
        return "forward:/index.html";
    }
}

