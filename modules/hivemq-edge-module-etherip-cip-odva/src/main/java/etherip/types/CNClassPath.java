/*******************************************************************************
 * Copyright (c) 2012 UT-Battelle, LLC.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package etherip.types;

import com.google.common.base.Objects;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Control Net Path for class, instance, attribute
 * <p>
 * Example (with suitable static import):
 * <p>
 * <code>CNPath path = Identity.instance(1).attr(7)</code>
 *
 * @author Kay Kasemir, László Pataki
 */
@SuppressWarnings("nls")
public class CNClassPath extends CNPath {
    protected int class_code;
    protected String class_name;
    protected int instance = 1, attr = 0;

    public CNClassPath() {}

    protected CNClassPath(final int class_code, final String class_name) {
        this.class_code = class_code;
        this.class_name = class_name;
    }

    public CNClassPath instance(final int instance) {
        this.instance = instance;
        return this;
    }

    public CNPath attr(final int attr) {
        this.attr = attr;
        return this;
    }

    /** @return Path length in words */
    public byte getPathLength() {
        return (byte) (getRequestSize() / 2);
    }

    @Override
    public int getRequestSize() {
        int size = 4; // a base path with 2 single byte elements
        if (shortClassId()) size += 2;
        if (shortInstanceId()) size += 2;
        if (hasAttribute()) {
            size += 2;
            if (shortAttributeId()) size += 2;
        }
        return size;
    }

    /** {@inheritDoc} */
    @Override
    public void encode(final ByteBuffer buf, final StringBuilder log) {
        buf.put(this.getPathLength());
        buf.put((byte) classSegmentType());
        if (shortClassId()) {
            buf.put((byte) 0); // Padding
            buf.putShort((short) this.class_code);
        } else buf.put((byte) this.class_code);

        buf.put((byte) instanceSegmentType());
        if (shortInstanceId()) {
            buf.put((byte) 0); // Padding
            buf.putShort((short) this.instance);
        } else buf.put((byte) this.instance);

        if (hasAttribute()) {
            buf.put((byte) attributeSegmentType());
            if (shortAttributeId()) {
                buf.put((byte) 0); // Padding
                buf.putShort((short) this.attr);
            } else buf.put((byte) this.attr);
        }
    }

    private byte classSegmentType() {
        if (shortClassId()) return 0x21;
        return 0x20;
    }

    private boolean shortClassId() {
        return this.class_code > 0xFF;
    }

    private byte instanceSegmentType() {
        if (shortInstanceId()) return 0x25;
        return 0x24;
    }

    private boolean shortInstanceId() {
        return this.instance > 0xFF;
    }

    private boolean hasAttribute() {
        return this.attr > 0;
    }

    private byte attributeSegmentType() {
        if (shortAttributeId()) return 0x31;
        return 0x30;
    }

    private boolean shortAttributeId() {
        return this.attr > 0xFF;
    }

    @Override
    public String toString() {
        StringBuilder description = new StringBuilder();
        description.append("Path ");
        if (hasAttribute()) description.append("(3 el)");
        else description.append("(2 el)");
        description
                .append(" Class(0x")
                .append(Integer.toHexString(classSegmentType()))
                .append(" ");
        description.append("0x").append(Integer.toHexString(this.class_code)).append(") ");
        description.append(this.class_name);

        description
                .append(", instance(0x")
                .append(Integer.toHexString(instanceSegmentType()))
                .append(") ")
                .append(this.instance);

        if (hasAttribute())
            description
                    .append(", attribute(0x")
                    .append(Integer.toHexString(attributeSegmentType()))
                    .append(") ")
                    .append(this.attr);
        return description.toString();
    }

    @Override
    public int getResponseSize(final ByteBuffer buf) throws Exception {
        return 2 + this.getPathLength() * 2;
    }

    @Override
    public void decode(final ByteBuffer buf, int available, final StringBuilder log) throws Exception {
        final byte[] raw = new byte[2];
        buf.get(raw);
        available = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN).getShort();

        if (raw[0] == 0x02) {
            buf.get(raw);
            if (raw[0] == 0x20) {
                this.class_code = (int) raw[1];
                this.class_name = "Ethernet Link";
            }
            buf.get(raw);
            if (raw[0] == 0x24) {
                this.instance((int) raw[1]);
            }
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof final CNClassPath that)) return false;
        return class_code == that.class_code
                && instance == that.instance
                && attr == that.attr
                && Objects.equal(class_name, that.class_name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(class_code, class_name, instance, attr);
    }

    public int getClass_code() {
        return class_code;
    }

    public int getInstance() {
        return instance;
    }

    public int getAttr() {
        return attr;
    }
}
