# Real-Time Updates and Testing Guide

This guide explains the real-time commit tracking features and how to test them.

## ✅ Fixed Issues

### 1. Real Streak Display
- **Problem**: Widget showed "Streak: 0 days" even with active commit streaks
- **Solution**: Widget now connects to the real CommitService and displays actual streak data
- **How it works**: The widget receives the commit history from the CommitService and calculates streaks based on daily commit counts

### 2. Real-Time Commit Updates
- **Problem**: New commits didn't appear in the expanded view immediately
- **Solution**: Widget now receives real-time updates when new commits are detected
- **How it works**: When the CommitService detects new commits, it updates the widget's commit history and refreshes the display

### 3. Start in Expanded Mode
- **Problem**: After Pokemon selection, widget started in compact mode
- **Solution**: Widget now automatically switches to expanded mode after Pokemon selection
- **Benefit**: Users immediately see their Pokemon status and commit history

## 🎮 Testing Features

### Keyboard Shortcuts (Testing Only)
Press these keys while the widget has focus:

- **I**: Show Pokemon info (including real XP and streak data)
- **F**: Force manual repository scan (refresh commit data immediately)
- **C**: Simulate a commit (for testing animations)
- **E**: Force evolution (for testing)
- **R**: Reset Pokemon to egg (for testing)
- **H/S/T**: Change Pokemon mood (Happy/Sad/Thriving)

### Real-Time Testing
1. **Make a commit** in any Git repository on your system
2. **Wait up to 10 seconds** for automatic detection, OR
3. **Press 'F'** to force an immediate scan
4. **Check the expanded view** to see the new commit appear
5. **Press 'I'** to see updated XP and streak values

## 🔍 Debug Information

### When you press 'I', you'll see:
```
🎮 Pokemon Info:
   Species: CHARMANDER
   Stage: EGG
   State: CONTENT
   Evolution in progress: false
   Real XP: 45
   Real Streak: 3 days
   Total commits in history: 12
   Last commit time: 2025-12-20T10:30:15
   Daily commit counts: 5 days
```

### Initial Startup Logs:
```
🔍 Initial commit history state:
   Total commits: 8
   Current streak: 3 days
   Daily commit counts: 4 days
   Last commit time: 2025-12-20T09:15:22
```

## 📊 How Streak Calculation Works

The streak is calculated based on consecutive days with commits:

1. **Today or Yesterday**: Must have commits to start a streak
2. **Consecutive Days**: Counts backward from today until a day with no commits
3. **Timezone Handling**: Accounts for commits made late at night/early morning

Example:
- Dec 20: 2 commits ✅
- Dec 19: 1 commit ✅  
- Dec 18: 3 commits ✅
- Dec 17: 0 commits ❌
- **Result**: 3-day streak

## 🚀 Real-Time Features

### Automatic Updates (Every 10 Seconds)
- Scans all Git repositories on your system
- Detects new commits automatically
- Updates XP, streak, and Pokemon state
- Refreshes the expanded view if open

### Manual Updates (Press 'F')
- Forces immediate repository scan
- Useful for testing or when you want instant updates
- Updates all displays immediately

### Live Display Updates
- **Compact Mode**: Pokemon animations reflect current state
- **Expanded Mode**: Shows real-time XP, streak, and commit list
- **Automatic Refresh**: Updates when switching between modes

## 🎯 Expected Behavior

### After Pokemon Selection:
1. Widget starts in **expanded mode**
2. Shows current Pokemon status (XP, level, streak)
3. Displays recent commit history
4. User can minimize to compact mode by double-clicking

### When Making Commits:
1. Commit is detected within 10 seconds (or immediately with 'F')
2. XP increases based on commit quality
3. Streak updates if it's a new day
4. Pokemon state may change based on activity
5. Expanded view shows the new commit in the list

### Real Data Sources:
- **XP**: Calculated from actual Git commits (6-10 XP per commit)
- **Streak**: Based on consecutive days with commits
- **Level**: Derived from total accumulated XP
- **Commit List**: Shows actual commit messages, timestamps, and repositories

## 🐛 Troubleshooting

### If streak shows 0:
1. Press 'I' to see debug info
2. Check if commits are being detected
3. Press 'F' to force a repository scan
4. Verify you have commits in the last few days

### If commits don't appear:
1. Make sure you have Git repositories in common locations
2. Check that repositories are accessible (not private without credentials)
3. Press 'F' to force a manual scan
4. Look at console output for error messages

### If XP doesn't update:
1. Verify commits are being detected (press 'I')
2. Check that the XP system is receiving commit data
3. Force a scan with 'F' to refresh data

The system now provides real-time, accurate tracking of your Git activity with immediate visual feedback in the Pokemon widget!