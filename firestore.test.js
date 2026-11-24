const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const STUDENT_A = "student_aarav";
const STUDENT_B = "student_ananya";
const ADMIN_UID = "nikhil_admin";
const ADMIN_EMAIL = "letswork2007@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function adminCtx() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("courses").where("published", "==", true).get());
  await assertFails(unauthDb.collection("communityPosts").where("hidden", "==", false).get());
  await assertFails(unauthDb.collection("users").doc(STUDENT_A).get());
  await assertFails(
    unauthDb.collection("users").doc(STUDENT_A).set({
      uid: STUDENT_A,
      name: "Unauth",
      email: "unauth@example.com",
      role: "student",
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("User registration enforces student role, blocks admin self-assignment, and isolates PII", async () => {
  const past = new Date(Date.now() - 5000);
  const aDb = testEnv.authenticatedContext(STUDENT_A).firestore();
  const bDb = testEnv.authenticatedContext(STUDENT_B).firestore();

  // Reject role escalation on create
  await assertFails(
    aDb.collection("users").doc(STUDENT_A).set({
      uid: STUDENT_A,
      name: "Aarav",
      email: "aarav@example.com",
      role: "admin",
      createdAt: past,
    })
  );

  // Valid student registration succeeds
  await assertSucceeds(
    aDb.collection("users").doc(STUDENT_A).set({
      uid: STUDENT_A,
      name: "Aarav Sharma",
      email: "aarav@example.com",
      role: "student",
      xp: 0,
      streakDays: 1,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Student B cannot read Student A's PII or list all users
  await assertFails(bDb.collection("users").doc(STUDENT_A).get());
  await assertFails(bDb.collection("users").get());

  // Student A cannot self-promote to admin on update
  await assertFails(
    aDb.collection("users").doc(STUDENT_A).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Student A can update own name & xp
  await assertSucceeds(
    aDb.collection("users").doc(STUDENT_A).update({
      name: "Aarav S.",
      xp: 120,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Spoofed unverified admin email is rejected; verified admin succeeds", async () => {
  const spoofDb = testEnv
    .authenticatedContext("attacker_uid", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();
  const past = new Date(Date.now() - 5000);

  await assertFails(
    spoofDb.collection("courses").doc("course_pencil").set({
      title: "Fake Course",
      description: "Spoof attempt",
      instructor: "Nikhil",
      category: "Pencil",
      price: 499,
      difficulty: "Beginner",
      duration: "4 weeks",
      totalSessions: 12,
      published: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  const realAdminDb = adminCtx().firestore();
  await assertSucceeds(
    realAdminDb.collection("courses").doc("course_pencil").set({
      title: "Pencil Sketching Fundamentals",
      description: "Learn basic sketching, shading, proportions and pencil techniques.",
      instructor: "Nikhil",
      thumbnailKey: "pencil",
      category: "Pencil Sketching",
      price: 499,
      difficulty: "Beginner",
      duration: "4 weeks",
      totalSessions: 12,
      curriculum: ["Tools & Line Control", "Shading & Value Scales"],
      learningObjectives: ["Draw confident proportions"],
      published: true,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Courses list query requires published == true for students; enrollments enforce deterministic ID and ownership", async () => {
  const past = new Date(Date.now() - 5000);
  const realAdminDb = adminCtx().firestore();
  await assertSucceeds(
    realAdminDb.collection("courses").doc("course_watercolor").set({
      title: "Watercolor Painting Masterclass",
      description: "Explore colour mixing, watercolor techniques, landscapes and creative compositions.",
      instructor: "Nikhil",
      thumbnailKey: "watercolor",
      category: "Watercolor",
      price: 799,
      difficulty: "Beginner to Intermediate",
      duration: "6 weeks",
      totalSessions: 18,
      published: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  const aDb = testEnv.authenticatedContext(STUDENT_A).firestore();
  const bDb = testEnv.authenticatedContext(STUDENT_B).firestore();

  // Aligned query succeeds, unfiltered query fails
  await assertSucceeds(aDb.collection("courses").where("published", "==", true).get());
  await assertFails(aDb.collection("courses").get());

  // Student A cannot enroll for non-existent course
  await assertFails(
    aDb.collection("enrollments").doc(`${STUDENT_A}_nonexistent`).set({
      studentId: STUDENT_A,
      studentName: "Aarav",
      courseId: "nonexistent",
      courseTitle: "Ghost",
      progress: 0,
      completedSessions: 0,
      completionStatus: "ongoing",
      enrollmentType: "demo",
      enrolledAt: past,
      updatedAt: past,
    })
  );

  // Student A enrolls in valid course using deterministic ID
  await assertSucceeds(
    aDb.collection("enrollments").doc(`${STUDENT_A}_course_watercolor`).set({
      studentId: STUDENT_A,
      studentName: "Aarav",
      courseId: "course_watercolor",
      courseTitle: "Watercolor Painting Masterclass",
      progress: 0,
      completedSessions: 0,
      completionStatus: "ongoing",
      enrollmentType: "demo",
      enrolledAt: past,
      updatedAt: past,
    })
  );

  // Student A updates own progress; Student B cannot read or modify it
  await assertSucceeds(
    aDb.collection("enrollments").doc(`${STUDENT_A}_course_watercolor`).update({
      progress: 33,
      completedSessions: 6,
      completionStatus: "ongoing",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bDb.collection("enrollments").doc(`${STUDENT_A}_course_watercolor`).get());
  await assertFails(aDb.collection("enrollments").get());
  await assertSucceeds(aDb.collection("enrollments").where("studentId", "==", STUDENT_A).get());
});

test("Community posts enforce author ownership, atomic like batches, comments, and moderation reports", async () => {
  const past = new Date(Date.now() - 5000);
  const aDb = testEnv.authenticatedContext(STUDENT_A).firestore();
  const bDb = testEnv.authenticatedContext(STUDENT_B).firestore();
  const realAdminDb = adminCtx().firestore();

  // Student A cannot publish a post marked demo=true
  await assertFails(
    aDb.collection("communityPosts").doc("post_fake_demo").set({
      userId: STUDENT_A,
      authorName: "Aarav",
      title: "Fake Demo",
      description: "Trying to spoof demo flag",
      category: "Watercolor",
      likesCount: 0,
      commentsCount: 0,
      demo: true,
      hidden: false,
      flagged: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Valid post creation by Student A
  await assertSucceeds(
    aDb.collection("communityPosts").doc("post_1").set({
      userId: STUDENT_A,
      authorName: "Aarav Sharma",
      title: "Sunset Study",
      description: "Quick watercolor study from today.",
      category: "Watercolor",
      likesCount: 0,
      commentsCount: 0,
      demo: false,
      hidden: false,
      flagged: false,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Aligned list succeeds, unfiltered list fails
  await assertSucceeds(bDb.collection("communityPosts").where("hidden", "==", false).get());
  await assertFails(bDb.collection("communityPosts").get());

  // Atomic batch like by Student B succeeds; standalone unbacked likesCount increment fails
  await assertFails(
    bDb.collection("communityPosts").doc("post_1").update({
      likesCount: 1,
    })
  );

  const likeBatch = bDb.batch();
  likeBatch.set(
    bDb.collection("communityPosts").doc("post_1").collection("likes").doc(STUDENT_B),
    {
      postId: "post_1",
      userId: STUDENT_B,
      createdAt: new Date(Date.now() - 1000),
    }
  );
  likeBatch.update(bDb.collection("communityPosts").doc("post_1"), {
    likesCount: 1,
  });
  await assertSucceeds(likeBatch.commit());

  // Student B comments on post_1
  await assertSucceeds(
    bDb.collection("communityPosts").doc("post_1").collection("comments").doc("c1").set({
      postId: "post_1",
      userId: STUDENT_B,
      authorName: "Ananya Patil",
      content: "Love the warm sky wash!",
      createdAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    bDb.collection("communityPosts").doc("post_1").collection("comments").where("postId", "==", "post_1").get()
  );

  // Student B cannot edit Student A's post title, nor delete it
  await assertFails(
    bDb.collection("communityPosts").doc("post_1").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bDb.collection("communityPosts").doc("post_1").delete());

  // Student B can report post_1; Admin can hide post_1
  await assertSucceeds(
    bDb.collection("reports").doc("rep_1").set({
      postId: "post_1",
      postTitle: "Sunset Study",
      reporterId: STUDENT_B,
      reason: "Test moderation report",
      status: "pending",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    realAdminDb.collection("communityPosts").doc("post_1").update({
      hidden: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Live classes can only be created/managed by Admin and queried safely by students", async () => {
  const past = new Date(Date.now() - 5000);
  const futureMillis = Date.now() + 3600000;
  const realAdminDb = adminCtx().firestore();
  const aDb = testEnv.authenticatedContext(STUDENT_A).firestore();

  await assertSucceeds(
    realAdminDb.collection("courses").doc("course_pencil").set({
      title: "Pencil Sketching Fundamentals",
      description: "Basics of sketching",
      instructor: "Nikhil",
      category: "Pencil Sketching",
      price: 499,
      difficulty: "Beginner",
      duration: "4 weeks",
      totalSessions: 12,
      published: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Student cannot create a live class
  await assertFails(
    aDb.collection("liveClasses").doc("cls_1").set({
      courseId: "course_pencil",
      courseTitle: "Pencil Sketching Fundamentals",
      title: "Student Class",
      description: "Unauthorized",
      instructor: "Nikhil",
      scheduledAtMillis: futureMillis,
      durationMinutes: 60,
      zoomMeetingId: "123456789",
      status: "upcoming",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Admin creates live class
  await assertSucceeds(
    realAdminDb.collection("liveClasses").doc("cls_1").set({
      courseId: "course_pencil",
      courseTitle: "Pencil Sketching Fundamentals",
      title: "Shading Spheres & Cast Shadows",
      description: "Interactive live Zoom workshop with Nikhil.",
      instructor: "Nikhil",
      scheduledAtMillis: futureMillis,
      durationMinutes: 60,
      zoomMeetingId: "84219304512",
      status: "upcoming",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Student query aligned with status in ['upcoming', 'live_now', 'completed'] succeeds
  await assertSucceeds(
    aDb
      .collection("liveClasses")
      .where("status", "in", ["upcoming", "live_now", "completed"])
      .get()
  );
});
