package info.kgeorgiy.ja.Davydov.student;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.GroupQuery;
import info.kgeorgiy.java.advanced.student.Student;

public class StudentDB implements GroupQuery {

    private static final Comparator<Student> STUDENTS_COMPARATOR_BY_NAME = Comparator
            .comparing(Student::lastName, Comparator.reverseOrder())
            .thenComparing(Student::firstName, Comparator.reverseOrder())
            .thenComparingInt(Student::id);

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return students.stream().map(Student::firstName).toList();
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return students.stream().map(Student::lastName).toList();
    }

    @Override
    public List<GroupName> getGroupNames(List<Student> students) {
        return students.stream().map(Student::groupName).toList();
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return students.stream().map(x -> x.firstName() + " " + x.lastName()).toList();
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getFirstNames(students).stream().collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Comparator.naturalOrder()).map(Student::firstName).orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return students.stream().sorted(Comparator.naturalOrder()).toList();
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return students.stream().sorted(STUDENTS_COMPARATOR_BY_NAME).toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return sortStudentsByName(students.stream().filter(x -> x.firstName().equals(name)).toList());
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return sortStudentsByName(students.stream().filter(x -> x.lastName().equals(name)).toList());
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return sortStudentsByName(students.stream().filter(x -> x.groupName().equals(group)).toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors.toMap(Student::lastName, Student::firstName,
                        BinaryOperator.minBy(Comparable::compareTo)));
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(Student::groupName)).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new Group(e.getKey(), sortStudentsByName(e.getValue())))
                .toList();
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(Student::groupName)).entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new Group(e.getKey(), sortStudentsById(e.getValue())))
                .toList();
    }

    @Override
    public GroupName getLargestGroup(Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(Student::groupName, Collectors.counting()))
                .entrySet().stream()
                .max(Comparator.<Map.Entry<GroupName, Long>, Long>comparing(Map.Entry::getValue)
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey).orElse(null);
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(Student::groupName,
                Collectors.mapping(Student::firstName, Collectors.toSet())))
                .entrySet().stream()
                .max(Comparator.<Map.Entry<GroupName, Set<String>>, Integer>comparing(e -> e.getValue().size())
                        .thenComparing(
                                Comparator.<Map.Entry<GroupName, Set<String>>, GroupName>comparing(Map.Entry::getKey)
                                        .reversed()))
                .map(Map.Entry::getKey).orElse(null);
    }
}