package info.kgeorgiy.ja.Davydov.student;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
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

    private <T> List<T> mapStudents(List<Student> students, Function<Student, T> func) {
        return students.stream().map(func).toList();
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapStudents(students, Student::firstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapStudents(students, Student::lastName);
    }

    @Override
    public List<GroupName> getGroupNames(List<Student> students) {
        return mapStudents(students, Student::groupName);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapStudents(students, x -> x.firstName() + " " + x.lastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return getFirstNames(students).stream().collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> students) {
        return students.stream().max(Comparator.naturalOrder()).map(Student::firstName).orElse("");
    }

    private List<Student> sortStudents(Collection<Student> students, Comparator<Student> comp) {
        return students.stream().sorted(comp).toList();
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortStudents(students, Comparator.naturalOrder());
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortStudents(students, STUDENTS_COMPARATOR_BY_NAME);
    }

    private List<Student> findStudentsByPredicate(Collection<Student> students, Predicate<Student> predicate) {
        return sortStudentsByName(students.stream().filter(predicate).toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return findStudentsByPredicate(students, x -> x.firstName().equals(name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return findStudentsByPredicate(students, x -> x.lastName().equals(name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByPredicate(students, x -> x.groupName().equals(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, GroupName group) {
        return findStudentsByGroup(students, group).stream()
                .collect(Collectors.toMap(Student::lastName, Student::firstName,
                        BinaryOperator.minBy(Comparable::compareTo)));
    }

    private List<Group> getGroupsWithSort(Collection<Student> students, UnaryOperator<List<Student>> sorter) {
        return students.stream()
                .collect(Collectors.groupingBy(Student::groupName))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(x -> new Group(x.getKey(), sorter.apply(x.getValue())))
                .toList();
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsWithSort(students, this::sortStudentsByName);
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