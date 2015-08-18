package io.jandy.web.api;

import com.google.common.io.Closer;
import io.jandy.domain.BranchRepository;
import io.jandy.domain.Build;
import io.jandy.domain.ProjectRepository;
import io.jandy.domain.java.JavaProfilingDump;
import io.jandy.domain.java.JavaProfilingDumpRepository;
import io.jandy.domain.java.JavaTreeNode;
import io.jandy.exception.ProjectNotRegisteredException;
import io.jandy.java.metrics.ProfilingMetrics;
import io.jandy.java.metrics.TreeNode;
import io.jandy.service.BuildService;
import io.jandy.service.java.JavaTreeNodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

import static java.util.stream.StreamSupport.stream;

/**
 * @author JCooky
 * @since 2015-07-06
 */
@RestController
@RequestMapping("/rest/travis")
public class TravisRestController {
  private static final Logger logger = LoggerFactory.getLogger(TravisRestController.class);

  @Autowired
  private JavaTreeNodeBuilder javaTreeNodeBuilder;
  @Autowired
  private BuildService buildService;
  @Autowired
  private JavaProfilingDumpRepository javaProfilingDumpRepository;

  @RequestMapping(value = "/java", method = RequestMethod.POST)
  @Transactional
  public void putResultsForJava(@RequestParam String ownerName,
                                @RequestParam String repoName,
                                @RequestParam String branchName,
                                @RequestParam Long buildId,
                                @RequestParam Long buildNum,
                                @RequestParam("results") MultipartFile results) throws IOException, ClassNotFoundException, ProjectNotRegisteredException {
    logger.debug("request /travis/java with ownerName: {}, repoName: {}, branchName: {}", ownerName, repoName, branchName);

    try (Closer closer = Closer.create()) {
      ObjectInputStream ois = closer.register(new ObjectInputStream(new GZIPInputStream(results.getInputStream())));
      ProfilingMetrics metrics = (ProfilingMetrics)ois.readObject();

      Build build = buildService.getBuildForTravis(buildId);
      if (build == null) {
        build = buildService.createBuild(ownerName, repoName, branchName, buildId, "java", buildNum);
        logger.trace("create the build: {}", build);
      }

      JavaProfilingDump dump = new JavaProfilingDump();
      dump.setRoot(javaTreeNodeBuilder.buildTreeNode(metrics.getRoot(), null));
      dump.setBuild(build);
      dump.setMaxTotalDuration(stream(dump.spliterator(), false).mapToLong(JavaTreeNode::getElapsedTime).max().getAsLong());
      javaProfilingDumpRepository.save(dump);

      logger.info("add profiling dump to build: {}", build);
    }
  }
}
