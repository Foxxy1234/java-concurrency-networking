package info.kgeorgiy.ja.Davydov.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

public class StudentDB implements GroupQuery {

    private static final Comparator<Student> STUDENTS_COMPARATOR_BY_NAME = Comparator
            .comparing(Student::lastName, Comparator.reverseOrder())
            .thenComparing(Student::firstName, Comparator.reverseOrder())
            .thenComparingInt(Student::id);

    private static <T> Stream<T> mapStudents(final List<Student> students, final Function<Student, T> func) {
        return students.stream().map(func);
    }

    @Override
    public List<String> getFirstNames(final List<Student> students) {
        return mapStudents(students, Student::firstName).toList();
    }

    @Override
    public List<String> getLastNames(final List<Student> students) {
        return mapStudents(students, Student::lastName).toList();
    }

    @Override
    public List<GroupName> getGroupNames(final List<Student> students) {
        return mapStudents(students, Student::groupName).toList();
    }

    // :NOTE: toList
    @Override
    public List<String> getFullNames(final List<Student> students) {
        return mapStudents(students, student -> student.firstName() + " " + student.lastName()).toList();
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> students) {
        return mapStudents(students, Student::firstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> students) {
        return students.stream().max(Comparator.naturalOrder()).map(Student::firstName).orElse("");
    }

    private static Stream<Student> sortedStudentsByName(final Stream<Student> stream) {
        return stream.sorted(STUDENTS_COMPARATOR_BY_NAME);
    }

    // :NOTE: copy-paste
    @Override
    public List<Student> sortStudentsById(final Collection<Student> students) {
        return students.stream().sorted(Comparator.naturalOrder()).toList();
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> students) {
        return sortedStudentsByName(students.stream()).toList();
    }

    private static <T> Stream<Student> filteredStudentsByField(final Collection<Student> students,
            final Function<Student, T> fieldGetter, final T value) {
        return students.stream().filter(s -> Objects.equals(fieldGetter.apply(s), value));
    }

    private static <T> List<Student> findStudentsByField(final Collection<Student> students,
            final Function<Student, T> fieldGetter, final T value) {
        return sortedStudentsByName(filteredStudentsByField(students, fieldGetter, value))
                .toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> students, final String name) {
        return findStudentsByField(students, Student::firstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> students, final String name) {
        return findStudentsByField(students, Student::lastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> students, final GroupName group) {
        return findStudentsByField(students, Student::groupName, group);
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> students, final GroupName group) {
        return filteredStudentsByField(students, Student::groupName, group)
                .collect(Collectors.toMap(Student::lastName, Student::firstName,
                        BinaryOperator.minBy(String::compareTo)));
    }

    private static Map<GroupName, List<Student>> groupStudents(final Collection<Student> students) {
        return students.stream().collect(Collectors.groupingBy(Student::groupName));
    }

    private static List<Group> getGroupsWithSort(
            final Collection<Student> students,
            final UnaryOperator<List<Student>> sorter) {
        return groupStudents(students)
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(x -> new Group(x.getKey(), sorter.apply(x.getValue())))
                .toList();
    }

    @Override
    public List<Group> getGroupsByName(final Collection<Student> students) {
        return getGroupsWithSort(students, this::sortStudentsByName);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> students) {
        return getGroupsWithSort(students, this::sortStudentsById);
    }

    private static <T extends Comparable<? super T>> GroupName getLargestGroupBy(
            final Collection<Student> students,
            final Function<List<Student>, T> calculateValue,
            final Comparator<GroupName> secondComparator) {
        return groupStudents(students)
                .entrySet().stream()
                .max(Comparator.<Map.Entry<GroupName, List<Student>>, T>comparing(
                        x -> calculateValue.apply(x.getValue()))
                        .thenComparing(Map.Entry::getKey, secondComparator))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Override
    public GroupName getLargestGroup(final Collection<Student> students) {
        return getLargestGroupBy(students, List::size, Comparator.naturalOrder());
    }

    @Override
    public GroupName getLargestGroupFirstName(final Collection<Student> students) {
        return getLargestGroupBy(
                students,
                // :NOTE: each time
                list -> mapStudents(list, Student::firstName).distinct().count(),
                Comparator.reverseOrder());
    }
}
