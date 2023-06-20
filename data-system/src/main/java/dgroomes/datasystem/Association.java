package dgroomes.datasystem;

/**
 * This is a key type. {@link Association} represents an association between two tables.
 */
sealed public interface Association {

    static None toNone() {
        return NONE;
    }

    static One toOne(int idx) {
        return new One(idx);
    }

    static Many toMany(int... indices) {
        return new Many(indices);
    }

    Association add(int idx);

    None NONE = new None();

    final class None implements Association {
        private None() {
        }

        @Override
        public Association add(int idx) {
            return new One(idx);
        }
    }

    record One(int idx) implements Association {

        @Override
        public Association add(int idx) {
            return new Many(new int[]{this.idx, idx});
        }
    }


    record Many(int[] indices) implements Association {
        @Override
        public Association add(int idx) {
            var newIndices = new int[indices.length + 1];
            System.arraycopy(indices, 0, newIndices, 0, indices.length);
            newIndices[indices.length] = idx;
            return new Many(newIndices);
        }
    }
}
