package com.valdisdot.jobscrapper;

/**
 * Represents a job position extracted from a job listing website.
 * Contains all relevant information about a job vacancy including title,
 * salary, company, description, and link.
 */
public class JobPosition {
    /**
     * The job title/position name
     */
    private String title;

    /**
     * The salary information (may be null)
     */
    private String salary;

    /**
     * The company name
     */
    private String company;

    /**
     * Job description text
     */
    private String description;

    /**
     * URL to the job listing page
     */
    private String link;

    /**
     * Category tag for the job position
     */
    private String tag;

    /**
     * Returns the job title
     *
     * @return the job title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the salary information
     *
     * @return the salary information
     */
    public String getSalary() {
        return salary;
    }

    /**
     * Returns the company name
     *
     * @return the company name
     */
    public String getCompany() {
        return company;
    }

    /**
     * Returns the job description
     *
     * @return the job description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the job listing URL
     *
     * @return the job listing URL
     */
    public String getLink() {
        return link;
    }

    /**
     * Returns the category tag
     *
     * @return the category tag
     */
    public String getTag() {
        return tag;
    }

    /**
     * Sets the job title
     *
     * @param title the job title
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets the salary information
     *
     * @param salary the salary information
     */
    public void setSalary(String salary) {
        this.salary = salary;
    }

    /**
     * Sets the company name
     *
     * @param company the company name
     */
    public void setCompany(String company) {
        this.company = company;
    }

    /**
     * Sets the job description
     *
     * @param description the job description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Sets the job listing URL
     *
     * @param link the job listing URL
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * Sets the category tag
     *
     * @param tag the category tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    @Override
    public String toString() {
        return "JobPosition{" +
                "title='" + title + '\'' +
                ", salary='" + salary + '\'' +
                ", company='" + company + '\'' +
                ", description='" + description + '\'' +
                ", link='" + link + '\'' +
                ", tag='" + tag + '\'' +
                '}';
    }
}
