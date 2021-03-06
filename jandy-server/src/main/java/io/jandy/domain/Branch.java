package io.jandy.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author JCooky
 * @since 2015-06-30
 */
@Entity
public class Branch {
  @Id
  @GeneratedValue
  private long id;

  private String name;

  @ManyToOne
  @JsonIgnore
  private Project project;

  @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "branch")
  @OrderBy("number DESC")
  private List<Build> builds = new ArrayList<>();

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Project getProject() {
    return project;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public List<Build> getBuilds() {
    return builds;
  }

  public void setBuilds(List<Build> builds) {
    this.builds = builds;
  }
}
