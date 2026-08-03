public final class MethodsAndPassByValueExample {
    static final class Person {
        String name;

        Person(String name) {
            this.name = name;
        }
    }

    static void rename(Person person) {
        person.name = "Updated";
    }

    static void replace(Person person) {
        person = new Person("Replacement");
    }

    public static void main(String[] args) {
        Person person = new Person("Original");
        rename(person);
        System.out.println(person.name);
        replace(person);
        System.out.println(person.name);
    }
}
