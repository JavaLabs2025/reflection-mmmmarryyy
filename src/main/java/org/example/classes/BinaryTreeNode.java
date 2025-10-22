package org.example.classes;

import org.example.annotations.Generatable;

@Generatable
public class BinaryTreeNode {
    private Integer data;
    private BinaryTreeNode left;
    private BinaryTreeNode right;

    public BinaryTreeNode(Integer data, BinaryTreeNode left, BinaryTreeNode right) {
        this.data = data;
        this.left = left;
        this.right = right;
    }

    public Integer getData() {
        return data;
    }

    public BinaryTreeNode getLeft() {
        return left;
    }

    public BinaryTreeNode getRight() {
        return right;
    }

    public void setLeft(BinaryTreeNode left) {
        this.left = left;
    }

    public void setRight(BinaryTreeNode right) {
        this.right = right;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("BinaryTreeNode{")
                .append("data=").append(data)
                .append(", left=").append(left != null ? left.toString() : "null")
                .append(", right=").append(right != null ? right.toString() : "null")
                .append("}")
                .toString();
    }
}
