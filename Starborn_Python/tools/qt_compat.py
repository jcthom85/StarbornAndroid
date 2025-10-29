# tools/qt_compat.py
# One import that works with either PyQt6 or PyQt5.
# Usage in your tools: `from qt_compat import *` (or import specific names).
# Provides Qt enum aliases so old-style names like Qt.AlignCenter still work on PyQt6.

try:
    # Prefer PyQt6 if available
    from PyQt6.QtCore import Qt, QTimer, QSize, QPoint, QRect, pyqtSignal, QEvent
    from PyQt6.QtGui import QAction, QIcon, QCloseEvent, QKeySequence, QPixmap, QPainter
    from PyQt6.QtWidgets import *
    IS_QT6 = True

    # ---- Back-compat aliases (Qt6 → Qt5-style names) ----
    # Alignment
    if not hasattr(Qt, "AlignLeft"):
        Qt.AlignLeft = Qt.AlignmentFlag.AlignLeft
        Qt.AlignRight = Qt.AlignmentFlag.AlignRight
        Qt.AlignHCenter = Qt.AlignmentFlag.AlignHCenter
        Qt.AlignVCenter = Qt.AlignmentFlag.AlignVCenter
        Qt.AlignCenter = Qt.AlignmentFlag.AlignCenter
    # Mouse buttons
    if not hasattr(Qt, "LeftButton"):
        Qt.LeftButton = Qt.MouseButton.LeftButton
        Qt.RightButton = Qt.MouseButton.RightButton
        Qt.MiddleButton = Qt.MouseButton.MiddleButton
    # Item flags
    if not hasattr(Qt, "ItemIsSelectable"):
        Qt.ItemIsSelectable = Qt.ItemFlag.ItemIsSelectable
        Qt.ItemIsEnabled = Qt.ItemFlag.ItemIsEnabled
        Qt.ItemIsEditable = Qt.ItemFlag.ItemIsEditable
        Qt.ItemIsDragEnabled = Qt.ItemFlag.ItemIsDragEnabled
        Qt.ItemIsDropEnabled = Qt.ItemFlag.ItemIsDropEnabled
    # Check state
    if not hasattr(Qt, "Checked"):
        Qt.Checked = Qt.CheckState.Checked
        Qt.Unchecked = Qt.CheckState.Unchecked
        Qt.PartiallyChecked = Qt.CheckState.PartiallyChecked
    # Orientation
    if not hasattr(Qt, "Vertical"):
        Qt.Vertical = Qt.Orientation.Vertical
        Qt.Horizontal = Qt.Orientation.Horizontal
    # Modifiers
    if not hasattr(Qt, "NoModifier"):
        Qt.NoModifier = Qt.KeyboardModifier.NoModifier

except ImportError:
    # Fallback to PyQt5
    from PyQt5.QtCore import Qt, QTimer, QSize, QPoint, QRect, pyqtSignal, QEvent
    from PyQt5.QtGui import QAction, QIcon, QCloseEvent, QKeySequence, QPixmap, QPainter
    from PyQt5.QtWidgets import *
    IS_QT6 = False
