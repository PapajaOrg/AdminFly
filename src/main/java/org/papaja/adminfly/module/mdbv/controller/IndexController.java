package org.papaja.adminfly.module.mdbv.controller;

import org.papaja.adminfly.module.mdbv.mongodb.data.PaginationData;
import org.papaja.adminfly.module.mdbv.mongodb.record.MapRecord;
import org.papaja.adminfly.module.mdbv.mongodb.service.RecordService;
import org.papaja.adminfly.module.mdbv.mysql.dto.SourceDto;
import org.papaja.adminfly.module.mdbv.mysql.dto.SourcePathDto;
import org.papaja.adminfly.module.mdbv.mysql.entity.SourcePath;
import org.papaja.adminfly.module.mdbv.mysql.service.SourceService;
import org.papaja.adminfly.module.mdbv.mysql.service.SourcePathService;
import org.papaja.adminfly.shared.controller.AbstractController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.isNull;

@Controller("mdbvIndexController")
@RequestMapping("/mdbv")
public class IndexController extends AbstractController {

    @Autowired
    private RecordService records;

    @Autowired
    private SourcePathService paths;

    @Autowired
    private SourceService sources;

    @RequestMapping
    public ModelAndView home() {
        return newRedirect("sources");
    }

    @PreAuthorize("hasAuthority('READ')")
    @RequestMapping("/sources")
    public ModelAndView sources(
        @RequestParam(value = "forced", required = false) boolean forced
    ) {
        ModelAndView mav = newView("sources/index");

        mav.addObject("items", sources.getAll());

        if (forced) {
            mav.addObject("message", messages.getErrorMessage("text.accessDenied"));
        }

        return mav;
    }

    @PreAuthorize("hasAuthority('READ')")
    @RequestMapping(value = "/sources/edit/{id}", method = RequestMethod.GET)
    public ModelAndView sourcesEdit(@PathVariable Integer id) {
        ModelAndView mav = newView("sources/index");

        mav.addObject("items", sources.getAll());
        mav.addObject("source", sources.getOne(id));

        return mav;
    }

    @PreAuthorize("hasAuthority('READ')")
    @RequestMapping(value = "/sources/select/{id}", method = RequestMethod.GET)
    public ModelAndView selectSources(
        @PathVariable Integer id, RedirectAttributes attributes
    ) {
        ModelAndView mav = newRedirect("sources");

        sources.setActiveSource(id);
        attributes.addFlashAttribute("message",
            messages.getSuccessMessage("mdbv.access.granted", sources.getActiveSource().getName()));

        return mav;
    }

    @PreAuthorize("hasAuthority('UPDATE')")
    @RequestMapping(value = {
            "/sources/edit/{id}", "/sources/edit"
    }, method = RequestMethod.POST)
    public ModelAndView sourcesSave(
        @PathVariable(required = false) Integer id, @Valid SourceDto dto,
        BindingResult result, RedirectAttributes attributes
    ) {
        ModelAndView mav = newRedirect(isNull(id) ? "sources" : format("sources/edit/%d", id));

        if (result.hasErrors()) {
            attributes.addFlashAttribute("result", result);
            attributes.addFlashAttribute("sources", dto);
        } else {
            sources.save(dto, sources.getOne(id));
            attributes.addFlashAttribute("message", messages.getSuccessMessage("record.saved", "sources"));
        }

        return mav;
    }

    @PreAuthorize("hasAuthority('REMOVE')")
    @RequestMapping(value = "/sources/remove/{id}", method = RequestMethod.GET)
    public ModelAndView sourcesRemove(
        @PathVariable Integer id, RedirectAttributes attributes
    ) {
        ModelAndView mav = newRedirect("sources");

        sources.remove(id);
        attributes.addFlashAttribute("message",
            messages.getSuccessMessage("record.removed.id", messages.getMessage("label.sources"), id));

        return mav;
    }

    @PreAuthorize("hasAuthority('READ')")
    @RequestMapping("/paths")
    public ModelAndView paths() {
        ModelAndView mav = newView("paths/index");

        if (sources.hasActiveSource()) {
            mav.addObject("items", paths.getPaths(sources.getActiveSource()));
            mav.addObject("types", SourcePath.Type.values());
        } else {
            mav = newRedirect("sources?forced=1");
        }

        return mav;
    }

    @PreAuthorize("hasAuthority('READ')")
    @RequestMapping(value = "/paths/edit/{id}", method = RequestMethod.GET)
    public ModelAndView pathsEdit(@PathVariable Integer id) {
        ModelAndView mav = newView("paths/index");

        if (sources.hasActiveSource()) {
            mav.addObject("path", paths.getPath(id));
            mav.addObject("items", paths.getPaths());
            mav.addObject("types", SourcePath.Type.values());
        } else {
            mav = newRedirect("sources?forced=1");
        }

        return mav;
    }

    @PreAuthorize("hasAuthority('REMOVE')")
    @RequestMapping(value = "/paths/remove/{id}", method = RequestMethod.GET)
    public ModelAndView pathsRemove(
        @PathVariable Integer id, RedirectAttributes attributes
    ) {
        ModelAndView mav = newRedirect("paths");

        if (sources.hasActiveSource()) {
            paths.remove(id);
            attributes.addFlashAttribute("message", messages.getSuccessMessage("record.removed.id", "path", id));
        } else {
            mav = newRedirect("sources?forced=1");
        }

        return mav;
    }

    @PreAuthorize("hasAuthority('UPDATE')")
    @RequestMapping(value = {
        "/paths/edit/{id}", "/paths/edit"
    }, method = RequestMethod.POST)
    public ModelAndView pathsSave(
        @PathVariable(required = false) Integer id, @Valid SourcePathDto dto,
        BindingResult result, RedirectAttributes attributes
    ) {
        ModelAndView mav = newRedirect("paths");

        if (sources.hasActiveSource()) {
            if (result.hasErrors()) {
                attributes.addFlashAttribute("result", result);
                attributes.addFlashAttribute("path", dto);
            } else {
                paths.save(dto, paths.getPath(id));
            }
        } else {
            mav = newRedirect("sources?forced=1");
        }

        return mav;
    }

    @PreAuthorize("hasAuthority('READ')")
    @RequestMapping("/records")
    public ModelAndView records(
        @RequestParam(value = "page", required = false, defaultValue = "1") Integer page
    ) {
        ModelAndView mav = newView("records/index");

        if (sources.hasActiveSource()) {
            List<MapRecord> records    = this.records.getRecords(page - 1);
            PaginationData  pagination = new PaginationData(this.records.count(), page, RecordService.DEFAULT_SIZE);

            mav.addObject("pagination", pagination);
            mav.addObject("records", records);
        } else {
            mav = newRedirect("sources?forced=1");
        }

        return mav;
    }

    @PreAuthorize("hasAuthority('READ')")
    @RequestMapping("/records/view/{objectId:[\\w\\d.\\-]+}")
    public ModelAndView recordView(
        @PathVariable(name = "objectId", required = false) String objectId
    ) {
        ModelAndView mav = newView("records/view");

        if (sources.hasActiveSource()) {
            System.out.println(records.getRecord(objectId));
        } else {
            mav = newRedirect("sources?forced=1");
        }

        return mav;
    }

}
